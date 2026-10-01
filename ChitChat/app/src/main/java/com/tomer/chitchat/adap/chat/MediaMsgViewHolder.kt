package com.tomer.chitchat.adap.chat

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.ChatViewEvents
import com.tomer.chitchat.adap.ClickEvents
import com.tomer.chitchat.databinding.MsgItemMediaBinding
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.room.MsgMediaType

class MediaMsgViewHolder(
    private val b: MsgItemMediaBinding,
    val callBack: ChatViewEvents
) : BaseChatViewHolder(b.root) {
    private val context: Context = b.root.context
    private val onCli = View.OnClickListener {
        when (it.id) {
            b.root.id -> callBack.onChatItemClicked(bindingAdapterPosition, ClickEvents.ROOT)
            b.layUpload.id -> callBack.onChatItemClicked(
                bindingAdapterPosition,
                ClickEvents.UPLOAD
            )

            b.layDownload.id -> callBack.onChatItemClicked(
                bindingAdapterPosition,
                ClickEvents.DOWNLOAD
            )

            b.repImgRv.id, b.tvRep.id -> callBack.onChatItemClicked(
                bindingAdapterPosition,
                ClickEvents.REPLY
            )

            b.mediaImg.id -> callBack.onChatItemClicked(
                bindingAdapterPosition,
                ClickEvents.IMAGE
            )
        }
    }

    private val onLongClickLis = View.OnLongClickListener {
        callBack.onChatItemLongClicked(bindingAdapterPosition)
        true
    }

    init {
        b.root.setOnClickListener(onCli)
        b.layUpload.setOnClickListener(onCli)
        b.layDownload.setOnClickListener(onCli)
        b.repImgRv.setOnClickListener(onCli)
        b.tvRep.setOnClickListener(onCli)
        b.mediaImg.setOnClickListener(onCli)


        b.root.setOnLongClickListener(onLongClickLis)
        b.mediaImg.setOnLongClickListener(onLongClickLis)
        b.repImgRv.setOnLongClickListener(onLongClickLis)
        b.tvRep.setOnLongClickListener(onLongClickLis)
    }

    override fun bind(mod: UiMsgModal) {

        val p = b.msgLay.layoutParams as ConstraintLayout.LayoutParams
        if (mod.isSent) {
            p.startToStart = ConstraintLayout.LayoutParams.UNSET
            p.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            b.contTime.gravity = Gravity.END
            b.msgLay.gravity = Gravity.END
            b.innerLay.gravity = Gravity.END
            b.imgMsgStatus.visibility = View.VISIBLE
            b.msgBg.setData(mod.isSent, corners, myColor)
            b.msgBg.foreground = ContextCompat.getDrawable(context, R.drawable.msg_sent)
        } else {
            p.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            p.endToEnd = ConstraintLayout.LayoutParams.UNSET
            b.contTime.gravity = Gravity.START
            b.msgLay.gravity = Gravity.START
            b.innerLay.gravity = Gravity.START
            b.imgMsgStatus.visibility = View.GONE
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

        b.tvRep.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.times(.86f))

        val params = b.mediaImg.layoutParams as ConstraintLayout.LayoutParams
        if (params.dimensionRatio == null) {
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        } else {
            params.height = 0
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.dimensionRatio = mod.aspectRatio.toString()
        }
        b.mediaImg.layoutParams = params

        Glide.with(context)
            .load(mod.bytes)
            .apply(options)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(b.mediaImg)
            .also { b.mediaImg.setCorners(corners) }

        if (mod.msgType == MsgMediaType.VIDEO) {
            b.layVideoTime.visibility = View.VISIBLE
            b.tvVideoTime.text = mod.info
            if (mod.isProg)
                b.imgPlayButton.visibility = View.GONE
            else b.imgPlayButton.visibility = View.VISIBLE
        } else {
            b.imgPlayButton.visibility = View.GONE
            b.layVideoTime.visibility = View.GONE
        }

        if (mod.isProg) {
            b.rvProg.visibility = View.VISIBLE
            b.layMediaRoot.visibility = View.VISIBLE
            b.layUpload.visibility = View.GONE
            b.layDownload.visibility = View.GONE
        } else {
            if (mod.isUploaded && mod.isDownloaded) {
                b.layMediaRoot.visibility = View.GONE
                return
            }
            b.rvProg.visibility = View.GONE
            b.layMediaRoot.visibility = View.VISIBLE
            if (mod.isSent) {//uploading
                b.layDownload.visibility = View.GONE
                if (mod.isUploaded) b.layMediaRoot.visibility = View.GONE
                else b.layUpload.visibility = View.VISIBLE

            } else {//downloading
                b.layUpload.visibility = View.GONE
                if (mod.isDownloaded) b.layMediaRoot.visibility = View.GONE
                else {
                    b.layDownload.visibility = View.VISIBLE
                    b.tvDownBytes.text = mod.mediaSize
                }
            }
        }

        b.tvTime.text = mod.timeText
        b.imgMsgStatus.setImageDrawable(statusDrawables[mod.status.ordinal])
        b.contTime.visibility = View.VISIBLE
        if (mod.status.ordinal == 2) b.contTime.visibility = View.GONE

        if (mod.isSelected)
            b.root.setBackgroundColor(ContextCompat.getColor(context, R.color.selected))
        else b.root.setBackgroundColor(ContextCompat.getColor(context, R.color.trans))
    }

}