package com.tomer.chitchat.room

import com.tomer.chitchat.adap.chat.ChatViewTypes
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.utils.EmojisHashingUtils

class ModelRoomMessageBuilder {
    private var id: Long = 0
    private var replyId: Long = 0
    private var partnerId: String = ""
    private var msgText: String = ""
    private var repText: String = ""
    private var msgStatus: MsgStatus = MsgStatus.SENDING
    private var msgType: MsgMediaType = MsgMediaType.TEXT
    private var replyType: MsgMediaType = MsgMediaType.TEXT
    private var isSent: Boolean = false
    private var isRep: Boolean = false

    private var bytes: ByteArray? = null
    private var repBytes: ByteArray? = null

    private var aspectRatio: Float? = null
    private var mediaName: String? = null
    private var mediaSize: String = ""
    private var replyMediaName: String? = null

    private var viewType = ChatViewTypes.TEXT
    private var timeText: String = ""
    private var info: String = ""
    private var timeMillis: Long = System.currentTimeMillis()

    fun id(id: Long) = apply { this.id = id }
    fun replyId(id: Long) = apply { this.replyId = id }
    fun setPartner(fromUser: String) = apply { this.partnerId = fromUser }
    fun msgText(msgText: String) = apply { this.msgText = msgText }
    fun repText(repText: String) = apply { this.repText = repText }
    fun msgStatus(msgStatus: MsgStatus) = apply { this.msgStatus = msgStatus }
    fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
    fun replyType(replyType: MsgMediaType) = apply { this.replyType = replyType }
    fun isSent(isSent: Boolean) = apply { this.isSent = isSent }
    fun isRep(isRep: Boolean) = apply { this.isRep = isRep }
    fun mediaFileName(fileName: String?) = apply { this.mediaName = fileName }
    fun mediaSize(mediaSize: String) = apply { this.mediaSize = mediaSize }
    fun replyMediaFileName(fileName: String?) = apply { this.replyMediaName = fileName }
    fun setBytes(bytes: ByteArray?) = apply { this.bytes = bytes }
    fun setRepBytes(repBytes: ByteArray?) = apply { this.repBytes = repBytes }
    fun setTimeText(time: String) = apply { this.timeText = time }
    fun setTimeMillis(time: Long) = apply { this.timeMillis = time }
    fun setInfo(info: String) = apply { this.info = info }
    fun setAspectRatio(ratio: Float?) = apply { this.aspectRatio = ratio }
    fun setViewType(viewType: ChatViewTypes) = apply { this.viewType = viewType }

    fun build() = ModelRoomMessage(
        id = id,
        replyId = replyId,
        partnerId = partnerId,
        msgText = msgText,
        repText = repText,
        msgStatus = msgStatus,
        msgType = msgType,
        replyType = replyType,
        mediaFileName = mediaName,
        mediaSize = mediaSize,
        replyMediaFileName = replyMediaName,
        isSent = isSent,
        isRep = isRep,
        timeMillis = timeMillis,
        info = info,
        aspectRatio = aspectRatio
    )

    fun buildUI() = UiMsgModal(
        id = id,
        replyId = replyId,
        status = msgStatus,
        msg = msgText,
        rep = repText,
        isSent = isSent,
        isReply = isRep,
        timeText = timeText,
        msgType = msgType,
        mediaFileName = mediaName,
        mediaSize = mediaSize,
        replyMediaFileName = replyMediaName,
        replyType = replyType,
        isUploaded = !isSent,
        isDownloaded = true,
        isProg = true,
        bytes = bytes,
        repBytes = repBytes,
        isEmojiOnly = EmojisHashingUtils.isOnlyEmojis(msgText),
        spannableString = null,
        aspectRatio = aspectRatio,
        info = info,
        viewType = viewType
    )
}