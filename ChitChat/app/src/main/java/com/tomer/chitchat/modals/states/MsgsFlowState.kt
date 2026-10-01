package com.tomer.chitchat.modals.states

import com.tomer.chitchat.utils.Utils
import java.io.File

sealed class MsgsFlowState(
    val fromUser: String,
    val data: UiMsgModal? = null,
    val msgId: Long? = null,
    val oldId: Long? = null,
    val type: FlowType,
    val fileGif: File? = null,
    val isLast: Boolean = true

) {
    data class ChatMessageFlowState(
        val modelMessage: UiMsgModal,
        val fromUserC: String,
        val iL: Boolean
    ) :
        MsgsFlowState(type = FlowType.MSG, data = modelMessage, fromUser = fromUserC, isLast = iL)

    data class IOFlowState(
        val id: Long,
        val ioType: FlowType,
        val fromUserC: String,
        val msgData: UiMsgModal? = null
    ) :
        MsgsFlowState(type = ioType, msgId = id, fromUser = fromUserC, data = msgData)

    data class MsgStatusFlowState(
        val newId: Long,
        val oId: Long,
        val ioType: FlowType,
        val fromUserC: String
    ) :
        MsgsFlowState(type = ioType, msgId = newId, oldId = oId, fromUser = fromUserC)

    data class PartnerEventsFlowState(
        val ioType: FlowType,
        val fromUserC: String,
        val millis: Long? = null
    ) :
        MsgsFlowState(type = ioType, fromUser = fromUserC, msgId = millis)

    data class ChangeGif(
        val file: File? = null,
        val modelMessage: UiMsgModal? = null,
        val typeF: FlowType = FlowType.CHANGE_GIF,
        val phone: String
    ) :
        MsgsFlowState(fromUser = phone, type = typeF, fileGif = file, data = modelMessage)

    data class OpenFileFlowState(val file: File, val typeF: FlowType, val timeMillis: Long) :
        MsgsFlowState(
            fromUser = Utils.currentPartner?.partnerId ?: "0000000000",
            type = typeF,
            fileGif = file,
            msgId = timeMillis
        )

    data class OpenVideoState(val mod: UiMsgModal?,val timeMillis: Long) :
        MsgsFlowState(
            fromUser = Utils.currentPartner?.partnerId ?: "0000000000",
            type = FlowType.OPEN_VIDEO,
            data = mod,
            msgId = timeMillis
        )

}

