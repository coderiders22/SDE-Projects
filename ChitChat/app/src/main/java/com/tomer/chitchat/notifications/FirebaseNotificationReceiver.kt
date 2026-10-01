package com.tomer.chitchat.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.MessageHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseNotificationReceiver : FirebaseMessagingService() {

    @Inject
    lateinit var retro: Api

    @Inject
    lateinit var repoMsgs: RepoMessages

    @Inject
    lateinit var repoPersons: RepoPersons

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var repoRelations: RepoRelations

    @Inject
    lateinit var cryptoService: CryptoService

    @Inject
    lateinit var repoStorage: RepoStorage


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                retro.updateNotificationToken(token)
            } catch (_: Exception) {
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        CoroutineScope(Dispatchers.IO).launch {
            MessageHandler(gson, repoMsgs, repoPersons, cryptoService, notificationService, repoRelations, repoStorage) { msg ->
                Log.d("TAG--", "NOTIFICATION MANAGER: $msg")
                if (msg.type == FlowType.MSG) {
                    val messageModel = msg.data ?: return@MessageHandler
                    val name = repoPersons.getPersonPref(msg.fromUser)?.name ?: msg.fromUser
                    notificationService.showNewMessageNotification(messageModel, msg.fromUser, name)
                } else if (msg.type == FlowType.SEND_PR) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            retro.sendAck(msg.msgId.toString(), msg.fromUser)
                        } catch (_: Exception) {
                        }
                    }
                } else if (msg.type == FlowType.SEND_BULK_REC) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            retro.sendAckBulk(msg.data!!.msg, msg.fromUser)
                        } catch (_: Exception) {
                        }
                    }
                }
            }.apply {
                try {
                    handelMsg(message.data["data"] ?: "")
                } catch (_: Exception) {
                }
            }
        }

    }


}