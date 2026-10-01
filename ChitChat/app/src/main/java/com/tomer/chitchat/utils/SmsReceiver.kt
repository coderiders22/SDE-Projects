package com.tomer.chitchat.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver() : BroadcastReceiver() {

    private var callBack:(String)->Unit = {}

    fun setOnOtpRececived(callBack:(String)->Unit){
        this.callBack = callBack
    }

    override fun onReceive(context: Context, intent: Intent) {
        val smss = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in smss){
            callBack.invoke(sms.messageBody)
        }
    }
}