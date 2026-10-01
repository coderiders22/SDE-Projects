package com.tomer.chitchat.viewmodals

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.assets.RepoAssets
import com.tomer.chitchat.crypto.CipherUtils
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.NewConnection
import com.tomer.chitchat.modals.prefs.PartnerPrefBuilder
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.room.ModelRoomPersonRelation
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.WebSocketHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject
import kotlin.random.Random


@HiltViewModel
class MainViewModal @Inject constructor(
    private val repoUtils: RepoUtils,
    private val repoPersons: RepoPersons,
    private val repoStorage: RepoStorage,
    private val repoAssets: RepoAssets,
    private val repoMsg: RepoMessages,
    private val retro: Api,
    private val repoRelations: RepoRelations,
    private val cryptoService: CryptoService,
    private val webSocket: WebSocketHandler
) : ViewModel() {

    //region WEBSOCKET FLOW

    fun closeWebSocket() {
        try {
            webSocket.closeConnection()
        } catch (_: Exception) {
        }
    }

    suspend fun connectNew(
        phone: String,
        openNextActivity: Boolean,
        mandatoryConnect: Boolean = true
    ) {
        if (!mandatoryConnect) {
            viewModelScope.launch {
                if (!phone.isDigitsOnly()) return@launch
                if (repoRelations.getRelation(phone) == null) connectNew(
                    phone,
                    openNextActivity,
                    true
                )
                else {
                    val oldPersons = repoPersons.getPersonByPhone(phone)
                    val oldPerf =
                        repoPersons.getPersonPref(phone) ?: PartnerPrefBuilder(phone, phone).build()
                            .also { repoPersons.insertPersonPref(it) }
                    if (oldPersons == null)
                        ModelRoomPersons(
                            phone, oldPerf.name,
                            MsgMediaType.TEXT, "", -Random.nextLong(120),
                            System.currentTimeMillis(),
                            lastSeenMillis = System.currentTimeMillis(),
                            isSent = false,
                            msgStatus = MsgStatus.RECEIVED
                        ).apply { repoPersons.insertPerson(this) }
                    if (openNextActivity)
                        flowMsgs.emit(
                            MsgsFlowState.PartnerEventsFlowState(
                                FlowType.OPEN_NEW_CONNECTION_ACTIVITY,
                                phone
                            )
                        )
                }
            }.join()
            return
        }
        viewModelScope.launch {
            if (!phone.isDigitsOnly()) return@launch
            val relation = ModelRoomPersonRelation(
                phone,
                isConnSent = true,
                isAccepted = false,
                isRejected = false
            )
            cryptoService.setCurrentPartner(phone)

            genKeyAndSendNotification(relation)
            val oldPref =
                repoPersons.getPersonPref(phone) ?: PartnerPrefBuilder(phone, phone).build()
            repoPersons.insertPersonPref(oldPref)
            if (openNextActivity)
                flowMsgs.emit(
                    MsgsFlowState.PartnerEventsFlowState(
                        FlowType.OPEN_NEW_CONNECTION_ACTIVITY,
                        phone
                    )
                )
            else flowMsgs.emit(
                MsgsFlowState.PartnerEventsFlowState(
                    FlowType.INCOMING_NEW_CONNECTION_REQUEST,
                    phone
                )
            )

            //getting Name from server
            val name = if (oldPref.name == phone) {
                try {
                    retro.getName(phone).body() ?: throw Exception()
                } catch (e: Exception) {
                    phone
                }
            } else oldPref.name
            oldPref.name = name
            repoPersons.insertPersonPref(oldPref)
            ModelRoomPersons(
                phone, name,
                MsgMediaType.TEXT, "Connection request sent...", -Random.nextLong(120),
                System.currentTimeMillis(),
                lastSeenMillis = System.currentTimeMillis(),
                isSent = false,
                msgStatus = MsgStatus.RECEIVED
            ).apply { repoPersons.insertPerson(this) }
        }.join()
    }

    private fun genKeyAndSendNotification(relation: ModelRoomPersonRelation) {
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

    val flowMsgs = MutableSharedFlow<MsgsFlowState>()

    init {
        Utils.myName = repoUtils.getPrefs().name
        viewModelScope.launch {
            webSocket.openConnection(repoUtils.getToken())
            webSocket.flowMsgs.collectLatest { msg ->
                flowMsgs.emit(msg)
            }
        }
    }

    //endregion WEBSOCKET FLOW

    private val _persons = MutableLiveData<List<PersonModel>>()
    val persons: LiveData<List<PersonModel>> = _persons

    fun loadMyDp() {
        viewModelScope.launch {
            flowMsgs.emit(
                MsgsFlowState.ChangeGif(
                    repoStorage.getDP(Utils.myPhone, false),
                    typeF = FlowType.SET_DP,
                    phone = Utils.myPhone
                )
            )
        }
    }

    //region WORK_MANAGER

    init {
        val prevTime = repoUtils.getTime()
        if (prevTime < System.currentTimeMillis()) {
            performSyncOperation()
            repoUtils.saveTime(System.currentTimeMillis() + 8_64_00_000)
        }
    }

    private fun performSyncOperation() {
        viewModelScope.launch {
            try {
                val noToGetDataSynced = repoPersons.getAllPersons().map { it.phoneNo }
                val sb = StringBuilder()
                for (i in noToGetDataSynced) {
                    if (sb.isNotEmpty()) sb.append(',')
                    sb.append(i)
                }
                val syncedData =
                    retro.getSyncedData(sb.toString()).body() ?: throw RuntimeException()
                for (i in syncedData) {
                    val prefMod = repoPersons.getPersonPref(i.phone) ?: continue
                    if (prefMod.dpNo != i.dpNo) {
                        repoStorage.deleteDP(i.phone)
                    }
                    prefMod.about = i.about
                    prefMod.dpNo = i.dpNo
                    repoPersons.insertPersonPref(prefMod)
                }
            } catch (_: Exception) {
            }
        }
    }

    //endregion WORK_MANAGER

    //region SELECTION HANDLING

    private val _fabView = MutableLiveData<Boolean>()
    val fabView: LiveData<Boolean> = _fabView
    fun setFab(show: Boolean) = _fabView.postValue(show)

    private val _headMenu = MutableLiveData<Boolean>()
    val headMenu: LiveData<Boolean> = _headMenu

    private val _selCount = MutableLiveData(0)
    val selCount: LiveData<Int> = _selCount

    private val selectedPhoneNos = mutableListOf<String>()

    fun addDelNo(phoneNo: String): Boolean {
        val i = selectedPhoneNos.indexOfFirst { phoneNo == it }
        return if (i == -1) {
            selectedPhoneNos.add(phoneNo)
            true
        } else {
            selectedPhoneNos.removeIf { phoneNo == it }
            false
        }.also {
            _selCount.postValue(selectedPhoneNos.size)
            if (selectedPhoneNos.isEmpty()) {
                if (headMenu.value != false)
                    _headMenu.postValue(false)
            } else
                if (headMenu.value != true)
                    _headMenu.postValue(true)
        }
    }

    fun delSelected(isDel: Boolean, oldList: List<PersonModel>) {
        if (isDel) {
            viewModelScope.launch {
                selectedPhoneNos.forEach {
                    launch { repoPersons.deletePersonById(it) }
                }
                loadPersons(oldList)
                withContext(Dispatchers.IO) {
                    selectedPhoneNos.forEach { no ->
                        launch {
                            val msgs = repoMsg.getMsgsOfUser(no, System.currentTimeMillis(), 1000)
                            msgs.forEach { msg ->
                                repoStorage.deleteFile(msg.mediaFileName, msg.msgType)
                            }
                            repoMsg.deleteAllByUser(no)
                        }
                    }
                }
            }
        }

        selectedPhoneNos.clear()
        _selCount.postValue(0)
        _headMenu.postValue(false)

    }

    //endregion SELECTION HANDLING

    //region LOAD PERSON DATA
    private suspend fun ModelRoomPersons.toUi(
        oldList: List<PersonModel>,
        needTobeDownload: MutableList<Pair<Int, String>>,
    ): PersonModel {
        val builder = PersonModel.Builder()
        builder.lastMsgId(lastMsgId)
        builder.name(name)
        builder.phoneNumber(phoneNo)
        builder.messageMediaType(mediaType)
        builder.lastDate(
            if (lastMsgId != -1L)
                ConversionUtils.getRelativeTime(timeMillis)
            else ""
        )
        builder.lastMessage(lastMsg)
        builder.unreadCount(unReadCount)
        builder.isSelected(false)
        builder.isOnline(lastSeenMillis == -1L)
        builder.isSent(isSent)
        builder.msgStatus(msgStatus)

        val prevSel = oldList.find { it.phoneNo == phoneNo }
        if (prevSel != null) builder.isSelected(prevSel.isSelected)

        val old = oldList.find { it.lastMsgId == lastMsgId }
        if (old != null) {
            builder.fileDp(old.fileDp)
            builder.lastMessageFile(old.fileGifImg)
            builder.jsonText(old.jsonText)
            builder.jsonName(old.jsonName)
            return builder.build()
        }

        builder.fileDp(
            repoStorage.getDP(phoneNo, true)
                .also { if (it == null) needTobeDownload.add(5 to phoneNo) })
        if (mediaType == MsgMediaType.EMOJI) {

            val nameGoogleJson = EmojisHashingUtils.googleJHash[ConversionUtils.encode(lastMsg)]
            if (!nameGoogleJson.isNullOrEmpty()) {
                builder.jsonText(repoAssets.getLottieJson(nameGoogleJson, true).also {
                    if (it == null) needTobeDownload.add(Pair(0, nameGoogleJson))
                } ?: "")
                builder.jsonName(nameGoogleJson)
                return builder.build()
            }

            val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(lastMsg)]
            if (!nameJson.isNullOrEmpty()) {
                builder.jsonText(repoAssets.getLottieJson(nameJson, true).also {
                    if (it == null) needTobeDownload.add(Pair(1, nameJson))
                } ?: "")
                builder.jsonName(nameJson)
                return builder.build()
            }

            val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(lastMsg)]
            if (!nameGif.isNullOrEmpty()) {
                builder.lastMessageFile(repoAssets.getGifFile(nameGif, true).also {
                    if (it == null) needTobeDownload.add(Pair(2, nameGif))
                })
                return builder.build()
            }

            val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(lastMsg)]
            if (!nameTeleGif.isNullOrEmpty()) {
                builder.lastMessageFile(
                    repoAssets.getGifFile(
                        ConversionUtils.encode(nameTeleGif),
                        true
                    ).also {
                        if (it == null) needTobeDownload.add(
                            Pair(
                                3,
                                ConversionUtils.encode(nameTeleGif)
                            )
                        )
                    })
                return builder.build()
            }

        } else if (mediaType == MsgMediaType.IMAGE || mediaType == MsgMediaType.GIF) {
            val msg =
                repoMsg.getMsg(lastMsgId) ?: return builder.messageMediaType(MsgMediaType.TEXT)
                    .build()
            val file = repoStorage.getFileFromFolder(mediaType, msg.mediaFileName.toString())
            if (file == null)
                builder.jsonText(msg.msgText.split(",-,")[1])
            else builder.lastMessageFile(file)

        } else if (mediaType == MsgMediaType.VIDEO) {
            val msg =
                repoMsg.getMsg(lastMsgId) ?: return builder.messageMediaType(MsgMediaType.TEXT)
                    .build()
            val file = repoStorage.getFileOfVideoThumb(msg.mediaFileName.toString())
            if (file == null)
                builder.jsonText(msg.msgText.split(",-,")[1])
            else builder.lastMessageFile(file)
        }
        return builder.build()
    }

    fun loadPersons(oldList: List<PersonModel>) {
        viewModelScope.launch {
            val needTobeDownload = mutableListOf<Pair<Int, String>>()
            val per = repoPersons.getAllPersons().map { it.toUi(oldList, needTobeDownload) }
            _persons.postValue(per)
            for (i in needTobeDownload) {
                async {
                    when (i.first) {
                        0 -> repoAssets.getGoogleLottieJson(i.second)
                        1 -> repoAssets.getLottieJson(i.second)
                        2 -> repoAssets.getGifFile(i.second)
                        3 -> repoAssets.getGifTelemoji(i.second)

                        else -> {
                            flowMsgs.emit(
                                MsgsFlowState.ChangeGif(
                                    repoStorage.getDP(
                                        i.second,
                                        false
                                    ), typeF = FlowType.SET_DP, phone = i.second
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun isNameSet() = repoUtils.getPrefs().name.isEmpty()
    fun getPhone() = repoUtils.getPrefs().phone

    //endregion LOAD PERSON DATA

    //region BIOMETRIC
    private val _openChatPhoneNo = MutableLiveData(Pair(false, ""))
    val openChatPhoneNo: LiveData<Pair<Boolean, String>> = _openChatPhoneNo

    fun openChat(phone: String) {
        if (phone.isEmpty()) {
            _openChatPhoneNo.postValue(Pair(false, ""))
            return
        }
        val partnerPref = repoPersons.getPersonPref(phone) ?: PartnerPrefBuilder("", "").build()
        _openChatPhoneNo.postValue(partnerPref.chatLocked to partnerPref.phone)
    }

    //endregion BIOMETRIC
}