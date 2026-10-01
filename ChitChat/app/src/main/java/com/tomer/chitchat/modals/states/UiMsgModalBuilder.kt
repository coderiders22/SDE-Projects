package com.tomer.chitchat.modals.states

import com.tomer.chitchat.adap.chat.ChatViewTypes
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.EmojisHashingUtils

class UiMsgModalBuilder {
    private var id: Long = 0
    private var replyId: Long = 0
    private var status: MsgStatus = MsgStatus.SENDING
    private var msg: String = ""
    private var rep: String = ""
    private var isSent: Boolean = false
    private var isReply: Boolean = false
    private var msgType: MsgMediaType = MsgMediaType.TEXT
    private var replyType: MsgMediaType = MsgMediaType.TEXT
    private var mediaName: String? = null
    private var mediaSize: String = ""
    private var replyMediaName: String? = null
    private var isDone: Boolean = false
    private var isDDone: Boolean = false
    private var isProg: Boolean = false
    private var bytes: ByteArray? = null
    private var repBytes: ByteArray? = null
    private var timeText: String = ""
    private var info: String = ""
    private var aspectRatio: Float? = null
    private var viewType = ChatViewTypes.TEXT

    fun id(id: Long) = apply { this.id = id }
    fun replyId(replyId: Long) = apply { this.replyId = replyId }
    fun status(status: MsgStatus) = apply { this.status = status }
    fun setMsg(msg: String) = apply { this.msg = msg }
    fun setRep(rep: String) = apply { this.rep = rep }
    fun isSent(isSent: Boolean) = apply {
        this.isSent = isSent
        isDone = !isSent
        isDDone = isSent
    }

    fun isReply(isReply: Boolean) = apply { this.isReply = isReply }
    fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
    fun replyType(replyType: MsgMediaType) = apply { this.replyType = replyType }
    fun mediaFileName(fileName: String?) = apply { this.mediaName = fileName }
    fun replyMediaFileName(fileName: String?) = apply { this.replyMediaName = fileName }
    fun isUploaded(isDone: Boolean) = apply { this.isDone = isDone }
    fun isDownloaded(isDDone: Boolean) = apply { this.isDDone = isDDone }
    fun isProg(isRetry: Boolean) = apply { this.isProg = isRetry }
    fun bytes(bytes: ByteArray?) = apply { this.bytes = bytes }
    fun repBytes(repBytes: ByteArray?) = apply { this.repBytes = repBytes }
    fun setTimeText(time: String) = apply { this.timeText = time }
    fun mediaSize(mediaSize: String) = apply { this.mediaSize = mediaSize }
    fun setInfo(info: String) = apply { this.info = info }
    fun setAspectRatio(ratio: Float?) = apply { this.aspectRatio = ratio }
    fun setViewType(viewType: ChatViewTypes) = apply { this.viewType = viewType }


    fun build() = UiMsgModal(
        id = id,
        replyId = replyId,
        status = status,
        msg = msg,
        rep = rep,
        timeText = timeText,
        isSent = isSent,
        isReply = isReply,
        msgType = msgType,
        replyType = replyType,
        isUploaded = isDone,
        isDownloaded = isDDone,
        isProg = isProg,
        bytes = bytes,
        mediaFileName = mediaName,
        mediaSize = mediaSize,
        replyMediaFileName = replyMediaName,
        repBytes = repBytes,
        isEmojiOnly = EmojisHashingUtils.isOnlyEmojis(msg),
        spannableString = null,
        aspectRatio = aspectRatio,
        info = info,
        viewType = viewType
    )
}