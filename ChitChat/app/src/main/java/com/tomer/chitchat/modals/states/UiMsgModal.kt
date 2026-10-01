package com.tomer.chitchat.modals.states

import android.text.SpannableString
import com.tomer.chitchat.adap.chat.ChatViewTypes
import com.tomer.chitchat.room.MsgMediaType

class UiMsgModal(
    var id: Long,
    val replyId: Long,
    var status: MsgStatus,
    val msg: String,
    val rep: String,
    val mediaFileName: String?,
    val mediaSize: String,
    val replyMediaFileName: String?,
    val timeText: String,
    var isSent: Boolean,
    val isReply: Boolean,
    val aspectRatio: Float?,
    val msgType: MsgMediaType,
    val replyType: MsgMediaType,
    var isUploaded: Boolean,
    var isDownloaded: Boolean,
    var isProg: Boolean,
    var bytes: ByteArray?,
    val repBytes: ByteArray?,
    val info: String,
    val viewType: ChatViewTypes,
    val isEmojiOnly: Boolean,
    var isSelected: Boolean = false,
    var spannableString: SpannableString?
) {
    override fun toString(): String {
        return "UiMsgModal(id=$id, replyId=$replyId, status=$status, msg='$msg', rep='$rep', mediaFileName=$mediaFileName, mediaSize='$mediaSize', replyMediaFileName=$replyMediaFileName, timeText='$timeText', isSent=$isSent, isReply=$isReply, aspectRatio=$aspectRatio, msgType=$msgType, replyType=$replyType, isUploaded=$isUploaded, isDownloaded=$isDownloaded, isProg=$isProg, bytes=${bytes.contentToString()}, repBytes=${repBytes.contentToString()}, info='$info', viewType=$viewType, isEmojiOnly=$isEmojiOnly, isSelected=$isSelected, spannableString=$spannableString)"
    }
}