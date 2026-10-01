package com.tomer.chitchat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.media3.common.util.UnstableApi
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.AdapPerson
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.ui.activities.ChatActivity

class AndroidNotificationService(
    private val context: Context,
    private val repoPersons: RepoPersons,
) : NotificationService {

    private val notiMan by lazy { NotificationManagerCompat.from(context) }

    @OptIn(UnstableApi::class)
    override fun showNewUserNotification(phonePartner: String, namePartner: String) {
        if (!notiMan.areNotificationsEnabled()) return
        if (notiMan.getNotificationChannelCompat("new_user") == null)
            createNewUserChannel()

        val i = Intent(context, ChatActivity::class.java)
        i.putExtra("phone", phonePartner)
        val pendingIntent = PendingIntentCompat.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT, false)

        val notification = NotificationCompat.Builder(context, "new_user")
            .setContentTitle("$namePartner • $phonePartner")
            .setSmallIcon(R.drawable.logo_noti3)
            .setContentText("sent you connection request 👋")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notiMan.notify(phonePartner.hashCode() + 10, notification)

    }

    @OptIn(UnstableApi::class)
    override fun showNewMessageNotification(msg: UiMsgModal?, phonePartner: String, namePartner: String) {
        if (!notiMan.areNotificationsEnabled()) return
        if (msg == null) return
        if (repoPersons.getPersonPref(phonePartner)?.notificationAllowed == false) return
        if (notiMan.getNotificationChannelCompat("new_msg") == null)
            createNewMsgChannel()

        val i = Intent(context, ChatActivity::class.java)
        i.putExtra("phone", phonePartner)
        val pendingIntent = PendingIntentCompat.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT, false)
//        val pendingIntent = TaskStackBuilder.create(context).run {
//            addNextIntentWithParentStack(i)
//            getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//        }

        val notification = NotificationCompat.Builder(context, "new_msg")
            .setContentTitle(namePartner)
            .setSmallIcon(R.drawable.logo_noti2)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val msgText = when (msg.msgType) {
            MsgMediaType.TEXT, MsgMediaType.EMOJI -> msg.msg
            MsgMediaType.IMAGE,
            MsgMediaType.GIF,
            MsgMediaType.FILE,
            MsgMediaType.VIDEO -> {
                try {
                    if (msg.bytes!!.size == 2) {
                        notification.setLargeIcon(Icon.createWithResource(context, AdapPerson.getDrawableId(msg.mediaFileName ?: "FILE")))
                    } else notification.setLargeIcon(BitmapFactory.decodeByteArray(msg.bytes, 0, msg.bytes!!.size))
                } catch (_: Exception) {
                }
                msg.mediaFileName
            }
        }
        notification.setContentText(msgText)
        notiMan.notify(phonePartner.hashCode(), notification.build())
    }


    private fun createNewMsgChannel() {
        val channel = NotificationChannel("new_msg", "New Message Notification", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Notification chanel for new incoming messages"
        channel.enableLights(true)
        channel.lightColor = Color.GRAY
        channel.enableVibration(true)

        notiMan.createNotificationChannel(channel)
    }

    private fun createNewUserChannel() {
        val channel = NotificationChannel("new_user", "New User Notification", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Notification chanel for new users to request connection"
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        channel.enableVibration(true)

        notiMan.createNotificationChannel(channel)
    }
}