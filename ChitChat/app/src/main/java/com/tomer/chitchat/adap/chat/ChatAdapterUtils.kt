package com.tomer.chitchat.adap.chat

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tomer.chitchat.R
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.utils.qrProvider.GradModel

@SuppressLint("CheckResult")
val options = RequestOptions().apply {
    placeholder(R.drawable.ic_gifs)
    override(320)
    error(R.drawable.logo)
    transform(RoundedCorners(8.px.toInt()))
}

var statusDrawables: List<Drawable> = emptyList()

var textSize: Float = 18f
var corners: Float = 12f
var partnerGrad: GradModel? = null

@ColorInt
var partnerColor: Int = 0

@ColorInt
var myColor: Int = 0

enum class ChatViewTypes {
    TEXT, TEXT_REPLY, EMOJI, MEDIA, FILE,
}

fun getChatViewType(msgType: MsgMediaType, isReply: Boolean): ChatViewTypes {
    if (msgType == MsgMediaType.TEXT) {
        return if (isReply) ChatViewTypes.TEXT_REPLY else ChatViewTypes.TEXT
    }
    if (msgType == MsgMediaType.EMOJI) return ChatViewTypes.EMOJI
    if (msgType == MsgMediaType.FILE) return ChatViewTypes.FILE
    return ChatViewTypes.MEDIA
}

abstract class BaseChatViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(mod: UiMsgModal)
}