package com.tomer.chitchat.viewmodals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tomer.chitchat.adap.chat.getChatViewType
import com.tomer.chitchat.crypto.CipherUtils
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.AcceptConnection
import com.tomer.chitchat.modals.msgs.ChatMessage
import com.tomer.chitchat.modals.msgs.Message
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.msgs.NewConnection
import com.tomer.chitchat.modals.msgs.OfflineStatus
import com.tomer.chitchat.modals.msgs.RejectConnection
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.modals.states.UiMsgModalBuilder
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.ModelRoomMessage
import com.tomer.chitchat.room.ModelRoomMessageBuilder
import com.tomer.chitchat.room.ModelRoomPersonRelation
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.WebSocketHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
class ChatViewModal @Inject constructor(
    private val repoUtils: RepoUtils,
    private val repoMsgs: RepoMessages,
    private val repoStorage: RepoStorage,
    private val repoPersons: RepoPersons,
    private val gson: Gson,
    private val notificationService: NotificationService,
    private val repoRelations: RepoRelations,
    private val cryptoService: CryptoService,
    private val webSocket: WebSocketHandler
) : ViewModel() {

    val flowMsgs = MutableSharedFlow<MsgsFlowState>()
    val chatMsgs = LinkedList<UiMsgModal>()

    var isChatActivityVisible = false
    var canSendMsg = false

    private var lastTime = System.currentTimeMillis()
    private val mutex = Mutex()

    private fun sendPendingMsgs() {
        val pendingMsgs = chatMsgs.filter {
            it.status == MsgStatus.SENDING
        }.map { it.id }

        viewModelScope.launch {
            pendingMsgs.reversed().forEach {
                val msgRoom = repoMsgs.getMsg(it)
                if (msgRoom != null && !msgRoom.msgText.startsWith('U')) {

                    val socketMsgBuilder = ModelMsgSocket.Builder()

                    socketMsgBuilder.replyId(msgRoom.replyId)
                    socketMsgBuilder.msgData(msgRoom.msgText)
                    socketMsgBuilder.replyData(msgRoom.repText)
                    socketMsgBuilder.msgType(msgRoom.msgType)
                    socketMsgBuilder.replyMsgType(msgRoom.replyType)
                    socketMsgBuilder.isReply(msgRoom.isRep)
                    socketMsgBuilder.setTimeMillis(System.currentTimeMillis())
                    socketMsgBuilder.replyMediaFileName(msgRoom.replyMediaFileName)
                    socketMsgBuilder.mediaFileName(msgRoom.mediaFileName)

                    try {
                        val encMsg = cryptoService.encString(gson.toJson(socketMsgBuilder.build()))
                            ?: return@launch
                        val actualMsg = "${Utils.currentPartner?.partnerId ?: "0000000000"}${
                            ChatMessage(
                                ConversionUtils.toBase64(it),
                                ConversionUtils.encode(encMsg)
                            )
                        }"
                        webSocket.sendMessage(actualMsg)
                    } catch (_: Exception) {
                    }
                }

            }
        }
    }

    init {
        viewModelScope.launch {
            webSocket.openConnection(repoUtils.getToken())
            webSocket.flowMsgs.collectLatest { msg ->
                if (Utils.currentPartner?.partnerId.toString() == msg.fromUser) {
                    if (!isChatActivityVisible && msg.type == FlowType.MSG)
                        notificationService.showNewMessageNotification(
                            msg.data,
                            msg.fromUser,
                            repoPersons.getPersonPref(msg.fromUser)?.name ?: msg.fromUser
                        )
                    if (isChatActivityVisible && msg.type == FlowType.MSG)
                        clearUnreadCount()

                    if (msg.type == FlowType.SERVER_REC) {
                        for (i in chatMsgs.indices) {
                            if (chatMsgs[i].id == msg.oldId!!) {
                                chatMsgs[i].id = msg.msgId!!
                                break
                            }
                        }
                    }
                    if (msg.type == FlowType.REQ_ACCEPTED) canSendMsg = true
                }
                flowMsgs.emit(msg)
            }
        }
        viewModelScope.launch {
            webSocket.flowConnection.collectLatest { isOpen ->
                if (isOpen) sendPendingMsgs()
            }
        }
    }

    //<(10)toPhone><(7)MSG_TYPE><DATA>
    fun sendMsg(msg: Message) {
        webSocket.sendMessage("${Utils.currentPartner?.partnerId ?: "0000000000"}$msg")
    }

    fun sendChatMsg(msg: ModelMsgSocket, replyBytes: ByteArray?) {
        viewModelScope.launch {
            val builder = ModelRoomMessageBuilder()
            builder.id(repoUtils.getTempId())
            builder.replyId(msg.replyId)
            builder.setPartner(Utils.currentPartner?.partnerId ?: "0000000000")
            builder.msgText(msg.msgData)
            builder.repText(msg.replyData)
            builder.msgType(msg.msgType)
            builder.replyType(msg.replyMsgType)
            builder.isSent(true)
            builder.isRep(msg.isReply)
            builder.setTimeMillis(msg.timeMillis)
            builder.setTimeText(ConversionUtils.millisToTimeText(msg.timeMillis))
            builder.mediaFileName(msg.mediaFileName)
            builder.replyMediaFileName(msg.replyMediaFileName)
            builder.setRepBytes(replyBytes)
            builder.setViewType(getChatViewType(msg.msgType, msg.isReply))

            val roomMsg = builder.build()
            repoMsgs.addMsg(roomMsg)
            try {
                val encMsg = cryptoService.encString(gson.toJson(msg)) ?: return@launch
                val actualMsg = "${Utils.currentPartner?.partnerId ?: "0000000000"}${
                    ChatMessage(
                        ConversionUtils.toBase64(roomMsg.id),
                        ConversionUtils.encode(encMsg)
                    )
                }"
                webSocket.sendMessage(actualMsg)
                try {
                    flowMsgs.emit(
                        MsgsFlowState.ChatMessageFlowState(
                            builder.buildUI(),
                            roomMsg.partnerId,
                            true
                        )
                    )
                    repoMsgs.addMsg(builder.build())
                    updatePersonModel(msg, builder.build().id)
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            }
        }
    }

    fun getTempId() = repoUtils.getTempId()

    fun sendMediaUploaded(msg: ModelMsgSocket, id: Long, toUser: String) {
        val encMsg = cryptoService.encString(gson.toJson(msg)) ?: return
        val actualMsg = "$toUser${
            ChatMessage(
                ConversionUtils.toBase64(id),
                ConversionUtils.encode(encMsg)
            )
        }"
        webSocket.sendMessage(actualMsg)
    }

    //region ACTIVITY COMM

    fun textChanged() {
        if (canSendMsg)
            webSocket.typing()
    }

    fun clearUnreadCount() {
        viewModelScope.launch {
            val per =
                repoPersons.getPersonByPhone(Utils.currentPartner!!.partnerId) ?: return@launch
            per.unReadCount = 0
            repoPersons.insertPerson(per)
        }
    }

    fun updatePersonModel(mod: ModelMsgSocket, tempId: Long) {
        viewModelScope.launch {
            val per =
                repoPersons.getPersonByPhone(Utils.currentPartner!!.partnerId) ?: return@launch
            val lastMsg: String = when (mod.msgType) {
                MsgMediaType.TEXT, MsgMediaType.EMOJI -> mod.msgData
                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> mod.mediaFileName
                    ?: mod.msgType.name
            }
            ModelRoomPersons(
                phoneNo = per.phoneNo,
                name = per.name,
                mediaType = mod.msgType,
                lastMsg = lastMsg,
                lastMsgId = tempId,
                timeMillis = mod.timeMillis,
                unReadCount = per.unReadCount,
                lastSeenMillis = per.lastSeenMillis,
                isSent = true,
                msgStatus = MsgStatus.SENDING
            ).also { repoPersons.insertPerson(it) }
        }
    }

    private fun ModelRoomMessage.toUI(): UiMsgModal {
        val builder = UiMsgModalBuilder()
            .id(id)
            .replyId(replyId)
            .status(msgStatus)
            .setMsg(msgText)
            .setRep(repText)
            .mediaFileName(mediaFileName)
            .replyMediaFileName(replyMediaFileName)
            .setTimeText(ConversionUtils.millisToTimeText(timeMillis))
            .isSent(isSent)
            .isReply(isRep)
            .msgType(msgType)
            .isProg(false)
            .replyType(replyType)
            .setAspectRatio(aspectRatio)
            .setInfo(info)
            .setViewType(getChatViewType(msgType, isRep))
            .mediaSize(mediaSize)
            .isUploaded(msgText.startsWith("http"))
            .isDownloaded(
                if (mediaFileName != null)
                    repoStorage.isPresent(
                        mediaFileName!!,
                        msgType
                    )
                else true
            )

        when (msgType) {
            MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                builder.bytes(
                    repoStorage.getBytesFromFolder(msgType, mediaFileName ?: "def")
                        ?: ConversionUtils.base64ToByteArr(msgText.split(",-,")[1])
                )
            }

            MsgMediaType.VIDEO -> builder.bytes(
                repoStorage.getBytesOfVideoThumb(mediaFileName ?: "def")
                    ?: ConversionUtils.base64ToByteArr(msgText.split(",-,")[1])
            )

            MsgMediaType.FILE -> builder.bytes(ByteArray(2))

            else -> {}
        }

        try {
            if (isRep)
                when (replyType) {
                    MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                        builder.repBytes(
                            repoStorage.getBytesFromFolder(replyType, replyMediaFileName ?: "def")
                                ?: ConversionUtils.base64ToByteArr(repText.split(",-,")[1])
                        )
                    }

                    MsgMediaType.VIDEO -> builder.repBytes(
                        repoStorage.getBytesOfVideoThumb(replyMediaFileName ?: "def")
                            ?: ConversionUtils.base64ToByteArr(repText.split(",-,")[1])
                    )

                    else -> {}
                }
        } catch (_: Exception) {
        }

        return builder.build()
    }

    var partnerPref: ModelPartnerPref? = null

    fun openChat(phone: String, selectedIds: MutableList<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launch { cryptoService.setCurrentPartner(phone) }
                val t1 = launch {
                    Utils.currentPartner = repoRelations.getRelation(phone)
                    canSendMsg = Utils.currentPartner?.isAccepted ?: false
                }
                partnerPref = repoPersons.getPersonPref(phone)
                t1.join()
                withContext(Dispatchers.Main) {
                    flowMsgs.emit(
                        MsgsFlowState.ChangeGif(
                            typeF = FlowType.SET_PREFS,
                            phone = phone
                        )
                    )
                }
            }
        }
        if (Utils.currentPartner?.partnerId == phone) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    flowMsgs.emit(
                        MsgsFlowState.IOFlowState(
                            0L,
                            FlowType.RELOAD_RV,
                            phone
                        )
                    )
                }
            }
            return
        }
        viewModelScope.launch {
            chatMsgs.clear()
            val newItems = loadMsgBeforeTime(selectedIds, System.currentTimeMillis(), 20, phone)
            chatMsgs.addAll(newItems)
            flowMsgs.emit(MsgsFlowState.IOFlowState(0L, FlowType.RELOAD_RV, phone))

            repoPersons.getPersonByPhone(phone)?.also {
                it.unReadCount = 0
                repoPersons.insertPerson(it)
            }
            if (Utils.currentPartner!!.isAccepted) {
                sendMsg(OfflineStatus())
                sendPendingMsgs()
            }
        }
    }

    private suspend fun loadMsgBeforeTime(
        selectedIds: MutableList<Long>,
        time: Long,
        limit: Int,
        phone: String = Utils.currentPartner!!.partnerId
    ): List<UiMsgModal> {
        return withContext(Dispatchers.IO) {
            selectedIds.sort()
            val newList = mutableListOf<UiMsgModal>()
            try {
                val roomMsgs = repoMsgs.getMsgsOfUser(
                    phone, time, limit
                )
                val arrUI = Array<UiMsgModal?>(roomMsgs.size) { _ -> null }

                val job = viewModelScope.launch {
                    val defList = mutableListOf<Deferred<Unit>>()
                    withContext(Dispatchers.IO) {
                        for (i in roomMsgs.indices) {
                            defList.add(async { arrUI[i] = roomMsgs[i].toUI() })
                        }
                        for (i in defList.indices) defList[i].join()
                    }
                }
                job.join()
                if (selectedIds.isEmpty())
                    for (a in arrUI) {
                        if (a != null)
                            newList.add(a)
                    }
                else for (a in arrUI) {
                    if (a == null) continue
                    val pos = selectedIds.binarySearch(a.id)
                    if (pos > -1) a.isSelected = true
                    newList.add(a)
                }
            } catch (e: Exception) {
                Log.e("TAG--", "loadMsgBeforeTime: ", e)
            }
            return@withContext newList
        }
    }

    fun genKeyAndSendNotification(relation: ModelRoomPersonRelation) {
        relation.isRejected = false
        repoRelations.saveRelation(relation)
        val key = cryptoService.checkForKeyAndGenerateIfNot(relation.partnerId)
        webSocket.sendMessage(
            "${relation.partnerId}${
                NewConnection(
                    CipherUtils.G.modPow(
                        BigInteger(key.tempKeyMy, 16),
                        CipherUtils.P
                    ).toString(16)
                )
            }"
        )
    }

    fun acceptConnection(accepted: Boolean) {
        if (accepted)
            sendMsg(
                AcceptConnection(
                    CipherUtils.G.modPow(
                        BigInteger(
                            cryptoService.checkForKeyAndGenerateIfNot(Utils.currentPartner!!.partnerId).tempKeyMy,
                            16
                        ), CipherUtils.P
                    ).toString(16)
                )
            ).also {
                cryptoService.setCurrentPartner(Utils.currentPartner!!.partnerId)
                canSendMsg = true
            }
        else sendMsg(RejectConnection())

        viewModelScope.launch {
            val relation = Utils.currentPartner!!
            if (accepted)
                relation.isAccepted = true
            else {
                relation.isConnSent = true
                relation.isAccepted = false
                relation.isRejected = true
            }
            repoRelations.saveRelation(relation)
            Utils.currentPartner = relation
        }
    }

    fun loadMoreData(limit: Int, selectedIds: MutableList<Long>) {
        if (chatMsgs.isEmpty()) return
        if (mutex.isLocked) return
        viewModelScope.launch {
            mutex.lock()
            try {
                val oldestMsgId = chatMsgs.last().id
                val oldestTime = repoMsgs.getMsg(oldestMsgId)?.timeMillis
                    ?: System.currentTimeMillis()
                if (lastTime == oldestTime) throw Exception("Same time")
                lastTime = oldestTime
                val olderItems = loadMsgBeforeTime(
                    selectedIds, oldestTime, limit
                )
                if (olderItems.isEmpty()) throw Exception("EMPTY LIST")

                chatMsgs.addAll(olderItems)
                flowMsgs.emit(
                    MsgsFlowState.IOFlowState(
                        olderItems.size.toLong(),
                        FlowType.RELOAD_RV_MORE,
                        Utils.currentPartner!!.partnerId
                    )
                )
            } catch (_: Exception) {
            } finally {
                mutex.unlock()
            }
        }
    }

    //endregion ACTIVITY COMM
}