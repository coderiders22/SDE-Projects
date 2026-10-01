package com.tomer.chitchat.modals.msgs

import com.google.gson.annotations.SerializedName
import com.tomer.chitchat.room.MsgMediaType

data class ModelMsgSocket(
    @SerializedName("r_i") val replyId: Long,
    @SerializedName("m_d") val msgData: String,
    @SerializedName("r_d") val replyData: String,
    @SerializedName("m_t") val msgType: MsgMediaType,
    @SerializedName("r_m_t") val replyMsgType: MsgMediaType,
    @SerializedName("is_r") val isReply: Boolean,
    @SerializedName("t_m") val timeMillis: Long,
    @SerializedName("a_r") val aspectRatio: Float?,
    @SerializedName("i") val info: String,
    @SerializedName("r_m_f_n") val replyMediaFileName: String?,
    @SerializedName("m_f_n") val mediaFileName: String?,
    @SerializedName("m_f_s") val mediaFileSize: String,
) {
    class Builder {
        private var replyId: Long = -1
        private var time: Long = System.currentTimeMillis()
        private var msgData: String = ""
        private var replyData: String = ""
        private var mediaName: String? = null
        private var replyMediaName: String? = null
        private var msgType: MsgMediaType = MsgMediaType.TEXT
        private var replyMsgType: MsgMediaType = MsgMediaType.TEXT
        private var isReply: Boolean = false
        private var mediaSize: String = ""
        private var aspectRatio: Float? = null
        private var info: String = ""

        fun replyId(replyId: Long) = apply { this.replyId = replyId }
        fun msgData(msgData: String) = apply { this.msgData = msgData }
        fun replyData(replyData: String) = apply { this.replyData = replyData }
        fun mediaFileName(fileName: String?) = apply { this.mediaName = fileName }
        fun replyMediaFileName(fileName: String?) = apply { this.replyMediaName = fileName }
        fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
        fun replyMsgType(replyMsgType: MsgMediaType) = apply { this.replyMsgType = replyMsgType }
        fun isReply(isReply: Boolean) = apply { this.isReply = isReply }
        fun setTimeMillis(time: Long) = apply { this.time = time }
        fun mediaSize(mediaSize: String) = apply { this.mediaSize = mediaSize }
        fun setInfo(info: String) = apply { this.info = info }
        fun setAspectRatio(ratio: Float?) = apply { this.aspectRatio = ratio }

        fun build() = ModelMsgSocket(
            replyId = replyId,
            msgData = msgData,
            replyData = replyData,
            mediaFileName = mediaName,
            replyMediaFileName = replyMediaName,
            msgType = msgType,
            replyMsgType = replyMsgType,
            isReply = isReply,
            timeMillis = time,
            mediaFileSize = mediaSize,
            info = info,
            aspectRatio = aspectRatio
        )
    }
}
