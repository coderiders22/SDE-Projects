package com.tomer.chitchat.adap.chat


import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.ChatViewEvents
import com.tomer.chitchat.adap.ClickEvents
import com.tomer.chitchat.databinding.MsgItemTextReplyBinding
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.room.MsgMediaType

class TextReplyMsgViewHolder(
    private val b: MsgItemTextReplyBinding,
    val callBack: ChatViewEvents
) : BaseChatViewHolder(b.root) {
    private val context: Context = b.root.context
    private val onCli = View.OnClickListener {
        when (it.id) {
            b.root.id, b.msgTv.id,b.innerLay.id -> callBack.onChatItemClicked(
                bindingAdapterPosition,
                ClickEvents.ROOT
            )

            b.repImgRv.id, b.tvRep.id -> callBack.onChatItemClicked(
                bindingAdapterPosition,
                ClickEvents.REPLY
            )
        }
    }

    private val onLongClickLis = View.OnLongClickListener {
        callBack.onChatItemLongClicked(bindingAdapterPosition)
        true
    }

    init {
        b.root.setOnClickListener(onCli)
        b.repImgRv.setOnClickListener(onCli)
        b.tvRep.setOnClickListener(onCli)
        b.msgTv.setOnClickListener(onCli)
        b.innerLay.setOnClickListener(onCli)


        b.root.setOnLongClickListener(onLongClickLis)
        b.msgTv.setOnLongClickListener(onLongClickLis)
        b.repImgRv.setOnLongClickListener(onLongClickLis)
        b.tvRep.setOnLongClickListener(onLongClickLis)
        b.innerLay.setOnLongClickListener(onLongClickLis)
    }

    override fun bind(mod: UiMsgModal) {
        //check for clickable Spanned
        if (mod.spannableString == null) {
            val spanStr = SpannableString(mod.msg)
            val matcher = Patterns.WEB_URL.matcher(mod.msg)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        callBack.onOpenLinkInBrowser(mod.msg.substring(start, end))
                    }
                }
                spanStr.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spanStr.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spanStr.setSpan(
                    ForegroundColorSpan("#ee0979".toColorInt()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            mod.spannableString = spanStr
        }
        b.msgTv.text = mod.spannableString
        b.msgTv.movementMethod = LinkMovementMethod.getInstance()
        val p = b.msgLay.layoutParams as ConstraintLayout.LayoutParams
        if (mod.isSent) {
            p.startToStart = ConstraintLayout.LayoutParams.UNSET
            p.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            b.contTime.gravity = Gravity.END
            b.msgLay.gravity = Gravity.END
            b.innerLay.gravity = Gravity.END
            b.msgTv.gravity = Gravity.END
            b.imgMsgStatus.visibility = View.VISIBLE
            b.msgBg.setData(mod.isSent, corners, myColor)
            b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.fore))
            b.msgBg.foreground = ContextCompat.getDrawable(context, R.drawable.msg_sent)
        } else {
            p.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            p.endToEnd = ConstraintLayout.LayoutParams.UNSET
            b.contTime.gravity = Gravity.START
            b.msgLay.gravity = Gravity.START
            b.innerLay.gravity = Gravity.START
            b.msgTv.gravity = Gravity.START
            b.imgMsgStatus.visibility = View.GONE
            b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            b.msgBg.foreground = ContextCompat.getDrawable(context, R.drawable.msg_rec)
            if (partnerGrad == null)
                b.msgBg.setData(mod.isSent, corners, partnerColor)
            else b.msgBg.setData(mod.isSent, corners, partnerGrad!!)
        }
        b.msgLay.layoutParams = p

        if (mod.isReply) {
            if (mod.replyType == MsgMediaType.GIF ||
                mod.replyType == MsgMediaType.IMAGE ||
                mod.replyType == MsgMediaType.VIDEO
            ) {
                b.repImgRv.visibility = View.VISIBLE
                b.tvRep.visibility = View.GONE
                Glide.with(context)
                    .asBitmap()
                    .load(mod.repBytes)
                    .apply(options)
                    .override(80)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(b.repImgRv)
            } else {
                b.repImgRv.visibility = View.GONE
                b.tvRep.visibility = View.VISIBLE
                b.tvRep.text = mod.rep
                b.tvRep.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.times(.86f))
                if (mod.replyType == MsgMediaType.FILE)
                    b.tvRep.text = mod.replyMediaFileName ?: "FILE"
            }
        } else {
            b.tvRep.visibility = View.GONE
            b.repImgRv.visibility = View.GONE
        }
        b.msgTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

        b.tvTime.text = mod.timeText
        b.imgMsgStatus.setImageDrawable(statusDrawables[mod.status.ordinal])
        b.contTime.visibility = View.VISIBLE
        if (mod.status.ordinal == 2) b.contTime.visibility = View.GONE

        if (mod.isSelected) b.root.setBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.selected
            )
        )
        else b.root.setBackgroundColor(ContextCompat.getColor(context, R.color.trans))
    }
}