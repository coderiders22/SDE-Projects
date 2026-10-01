package com.tomer.chitchat.utils

import android.util.Log
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.BulkReceived
import com.tomer.chitchat.modals.msgs.NoTyping
import com.tomer.chitchat.modals.msgs.Typing
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketHandler(
    gson: Gson,
    repoMsgs: RepoMessages,
    repoStorage: RepoStorage,
    repoPersons: RepoPersons,
    repoRelations: RepoRelations,
    cryptoService: CryptoService,
    private val notificationService: NotificationService,
) {

    //region HANDEL FLOWS
    val flowMsgs = MutableSharedFlow<MsgsFlowState>()
    val flowConnection = MutableSharedFlow<Boolean>()
    private val msgHandler = MessageHandler(gson, repoMsgs, repoPersons, cryptoService, notificationService, repoRelations, repoStorage) { msg ->
        if (msg.type == FlowType.PONG) {
            lastReceivedPong = System.currentTimeMillis()
            return@MessageHandler
        }
        CoroutineScope(Dispatchers.IO).launch {
            flowMsgs.emit(msg)
        }
        if (msg.type == FlowType.MSG && Utils.currentPartner?.partnerId.toString() != msg.fromUser) {
            CoroutineScope(Dispatchers.IO).launch {
                val name = repoPersons.getPersonPref(msg.fromUser)?.name ?: msg.fromUser
                notificationService.showNewMessageNotification(msg.data, msg.fromUser, name)
            }
        } else if (msg.type == FlowType.SEND_PR)
            sendMessage("${msg.fromUser}*ACK-PR${msg.msgId}")
        else if (msg.type == FlowType.SEND_BULK_REC)
            sendMessage(BulkReceived(msg.fromUser, msg.data?.msg ?: "").toString())
    }

    //endregion HANDEL FLOWS

    //region GLOBALS

    private var webSocket: WebSocket? = null
    private var token = ""
    private var closedByActivityEnd = false

    private val webSocketListener = object : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            tryReconnectAfter2Sec()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            tryReconnectAfter2Sec()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            //Handle Chunks IMPL
            if (text.elementAt(0) == 'C') {
                val chunkId = ConversionUtils.fromBase64(text.substring(1, 13))
                val chunkIndex = text.substring(13, 16).toInt()
                val chunkSize = text.substring(16, 19).toInt()

                val arr = chunkHash[chunkId]
                if (arr == null) {
                    val arr1 = Array(chunkSize) { "" }
                    chunkHash[chunkId] = arr1
                    arr1[chunkIndex] = text.substring(19)
                    return
                }
                arr[chunkIndex] = text.substring(19)

                for (i in arr) {
                    if (i.isEmpty()) return
                }

                val strB = StringBuilder()
                for (i in arr) strB.append(i)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        msgHandler.handelMsg(toString())
                    } catch (_: Exception) {
                    }
                }
                return
            }

            //Normal Msg IMPL
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    msgHandler.handelMsg(text)
                } catch (e: Exception) {
                    Log.e("TAG--", "onMessage: ", e)
                }
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            CoroutineScope(Dispatchers.IO).launch {
                flowConnection.emit(true)
                backOffMultiplier = 0
                Log.d("TAG--", "onOpen: ")
            }
        }
    }

    private var backOffMultiplier = 0
    private fun tryReconnectAfter2Sec() {
        if (closedByActivityEnd) {
            webSocket = null
            return
        }

        if (token.isNotEmpty())
            retryJob = CoroutineScope(Dispatchers.IO).launch {
                backOffMultiplier++
                Log.d("TAG--", "tryReconnectAfter2Sec: ")
                webSocket = null
                delay(2000L * backOffMultiplier)
                openConnection(token)
            }
    }

    private var pingingJob: Job = createNewPingingJob()
    private var retryJob: Job? = null


    private var lastReceivedPong = System.currentTimeMillis()
    private fun createPongCheckJob(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            delay(30_000L)
            if ((System.currentTimeMillis() - lastReceivedPong) > 30_000L) webSocket?.close(1000, "PONG NOT RECEIVED")
        }
    }

    private fun createNewPingingJob(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(150_000)
                if (webSocket != null) {
                    webSocket?.send("PING")
                    createPongCheckJob()
                }
            }
        }
    }

    val chunkHash = HashMap<Long, Array<String>>()

    //endregion GLOBALS

    //region TYPING JOB

    private var typingJob: Job = CoroutineScope(Dispatchers.Default).launch { delay(100) }

    private fun createNewTypingJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            delay(2000)
            sendMessage((Utils.currentPartner?.partnerId ?: "0000000000") + NoTyping())
        }
    }

    fun typing() {
        if (!typingJob.isActive) {
            sendMessage((Utils.currentPartner?.partnerId ?: "0000000000") + Typing())
            typingJob = createNewTypingJob()
            return
        }
        typingJob.cancel()
        typingJob = createNewTypingJob()
    }
    //endregion TYPING JOB

    //region COMMU

    fun sendMessage(text: String) {
        if (webSocket == null) {
            CoroutineScope(Dispatchers.IO).launch { openConnection(token) }
            return
        }
        webSocket?.send(text)
    }

    suspend fun openConnection(token: String) {
        this.token = token
        closedByActivityEnd = false
        if (webSocket != null) return
        withContext(Dispatchers.IO) {
            webSocket = try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(Utils.WEBSOCKET_LINK)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                pingingJob.cancel()
                pingingJob = createNewPingingJob()
                client.newWebSocket(request, webSocketListener)
            } catch (e: Exception) {
                pingingJob.cancel()
                null
            }
        }
    }

    @Throws(Exception::class)
    fun closeConnection() {
        closedByActivityEnd = true
        try {
            webSocket!!.close(1001, "Activity closed")
        } catch (_: Exception) {
        }
        pingingJob.cancel()
        retryJob?.cancel()
    }

    //endregion COMMU
}
