package com.tomer.chitchat.notifications

import com.tomer.chitchat.modals.states.UiMsgModal

interface NotificationService {

    fun showNewUserNotification(phonePartner:String,namePartner:String)
    fun showNewMessageNotification(msg: UiMsgModal?, phonePartner: String, namePartner: String)
}