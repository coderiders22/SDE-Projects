package com.tomer.chitchat.adap.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.ChatViewEvents
import com.tomer.chitchat.databinding.MsgItemEmojiBinding
import com.tomer.chitchat.databinding.MsgItemFileBinding
import com.tomer.chitchat.databinding.MsgItemMediaBinding
import com.tomer.chitchat.databinding.MsgItemTextBinding
import com.tomer.chitchat.databinding.MsgItemTextReplyBinding
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.utils.qrProvider.GradModel
import java.util.LinkedList

class ChatAdapter(
    context: Context,
    private val callBack: ChatViewEvents,
    private val chatItems: LinkedList<UiMsgModal>
) : RecyclerView.Adapter<BaseChatViewHolder>() {

    init {
        myColor = ContextCompat.getColor(context, R.color.softBg)
        statusDrawables = listOf(
            ContextCompat.getDrawable(context, R.drawable.ic_sending)!!,
            ContextCompat.getDrawable(context, R.drawable.ic_tick)!!,
            ContextCompat.getDrawable(context, R.drawable.ic_double_tick)!!,
        )
    }

    override fun getItemId(position: Int) = chatItems[position].id

    override fun getItemViewType(position: Int): Int {
        return chatItems[position].viewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChatViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ChatViewTypes.TEXT.ordinal -> TextMsgViewHolder(
                MsgItemTextBinding.inflate(inflater, parent, false), callBack
            )

            ChatViewTypes.TEXT_REPLY.ordinal -> TextReplyMsgViewHolder(
                MsgItemTextReplyBinding.inflate(
                    inflater, parent, false
                ), callBack
            )

            ChatViewTypes.EMOJI.ordinal -> EmojiMsgViewHolder(
                MsgItemEmojiBinding.inflate(
                    inflater, parent, false
                ), callBack
            )

            ChatViewTypes.FILE.ordinal -> FileMsgViewHolder(
                MsgItemFileBinding.inflate(
                    inflater, parent, false
                ), callBack
            )

            else -> MediaMsgViewHolder(
                MsgItemMediaBinding.inflate(
                    inflater, parent, false
                ), callBack
            )
        }
    }

    override fun onBindViewHolder(holder: BaseChatViewHolder, position: Int) {
        val mod = chatItems[position]
        holder.bind(mod)
    }

    override fun getItemCount(): Int = chatItems.size

    //region COMMUNICATION

    fun addItem(msg: UiMsgModal) {
        chatItems.addFirst(msg)
        notifyItemInserted(0)
    }

    fun setValues(textSize: Float, corners: Float, gradModel: GradModel) {
        com.tomer.chitchat.adap.chat.textSize = textSize
        com.tomer.chitchat.adap.chat.corners = corners
        partnerGrad = gradModel
    }

    fun setValues(textSize: Float, corners: Float, @ColorInt color: Int) {
        com.tomer.chitchat.adap.chat.textSize = textSize
        com.tomer.chitchat.adap.chat.corners = corners
        partnerGrad = null
        partnerColor = color
    }

    //endregion COMMUNICATION

}