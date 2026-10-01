package com.tomer.chitchat.adap

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.RowPersonBinding
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils

class AdapPerson(
    private val clickLis: CallbackClick,
    private val context: Context
) :
    ListAdapter<PersonModel, AdapPerson.PersonHolder>(PersonDiff()) {

    private val selCol = ContextCompat.getColor(context, R.color.primary_light)
    private val deSelCol = ContextCompat.getColor(context, R.color.backgroundC)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PersonHolder(
        RowPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        clickLis
    )


    override fun onBindViewHolder(holder: PersonHolder, position: Int) {
        val model = getItem(position)
        holder.b.tvName.text = model.name.ifEmpty { model.phoneNo }
        holder.b.tvTime.text = model.lastDate
        holder.b.tvLastMsg.text = model.lastMsg.also { holder.b.tvLastMsg.tag = it }

        if (model.isSelected)
            holder.b.root.setBackgroundColor(selCol)
        else holder.b.root.setBackgroundColor(deSelCol)

        holder.b.onlineIndi.setImmediateStatus(model.isOnline)

        Glide.with(holder.b.imgProfile)
            .asBitmap()
            .load(model.fileDp)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .circleCrop()
            .override(200)
            .placeholder(R.drawable.def_avatar)
            .error(R.drawable.def_avatar)
            .into(holder.b.imgProfile)

        holder.b.tvUnreadMsgCount.text = model.unreadCount.toString()
        if (model.unreadCount > 0) {
            holder.b.tvUnreadMsgCount.visibility = View.VISIBLE
            holder.b.tvTime.setTextColor(ContextCompat.getColor(context, R.color.purple))
            holder.b.tvLastMsg.setTextColor(ContextCompat.getColor(context, R.color.purple))
        } else {
            holder.b.tvUnreadMsgCount.visibility = View.GONE
            holder.b.tvTime.setTextColor(ContextCompat.getColor(context, R.color.hintCol))
            holder.b.tvLastMsg.setTextColor(ContextCompat.getColor(context, R.color.hintCol))
        }

        if (model.isSent) {
            holder.b.msgStatus.visibility = View.VISIBLE
            holder.b.msgStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    context, when (model.msgStatus) {
                        MsgStatus.SENDING -> R.drawable.ic_sending
                        MsgStatus.SENT_TO_SERVER -> R.drawable.ic_tick
                        MsgStatus.RECEIVED -> R.drawable.ic_double_tick
                        MsgStatus.READ_BY_USER -> R.drawable.ic_double_tick
                    }
                )
            )
        } else holder.b.msgStatus.visibility = View.GONE


        when (model.mediaType) {
            MsgMediaType.EMOJI -> {
                holder.b.msgType.visibility = View.GONE
                if (model.fileGifImg == null) {
                    if (model.jsonText.isNotEmpty()) {
                        holder.b.imgLottie.setAnimationFromJson(model.jsonText, model.jsonName)
                        holder.b.imgLottie.playAnimation()
                    }
                } else {
                    Glide.with(context)
                        .load(model.fileGifImg)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(holder.b.imgLottie)
                }
            }

            MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO -> {
                holder.b.msgType.visibility = View.VISIBLE
                holder.b.msgType.setImageResource(getDrawableId(model.lastMsg))
                if (model.fileGifImg == null)
                    Glide.with(context)
                        .asBitmap()
                        .load(getByteArr(model.jsonText))
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(holder.b.imgLottie)
                else
                    Glide.with(context)
                        .load(model.fileGifImg)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(holder.b.imgLottie)
            }

            else -> {
                if (model.mediaType == MsgMediaType.FILE) {
                    holder.b.msgType.visibility = View.VISIBLE
                    holder.b.msgType.setImageResource(getDrawableId(model.lastMsg))
                    holder.b.imgLottie.setImageResource(getDrawableId(model.lastMsg))
                } else {
                    holder.b.msgType.visibility = View.GONE
                    holder.b.imgLottie.setImageDrawable(null)
                }
            }
        }

    }

    //region MAP ICON PROVIDER
    companion object {
        fun getByteArr(data: String): Any {
            return try {
                ConversionUtils.base64ToByteArr(data)
            } catch (e: Exception) {
                Log.e("TAG--", "getByteArr: ", e)
                return R.drawable.ic_send
            }
        }

        fun getDrawableId(name: String): Int {
            val ind = name.lastIndexOf(".")
            if (ind != -1) return getHashMap()[name.substring(ind + 1)] ?: R.drawable.ic_file
            return when (name) {
                MsgMediaType.IMAGE.name, MsgMediaType.GIF.name -> R.drawable.ic_image
                MsgMediaType.VIDEO.name -> R.drawable.ic_video
                else -> R.drawable.ic_file
            }
        }

        private val map = mutableMapOf<String, Int>()

        private fun getHashMap(): Map<String, Int> {
            if (map.isNotEmpty()) return map
            map["webp"] = R.drawable.ic_image
            map["jpg"] = R.drawable.ic_image
            map["jpeg"] = R.drawable.ic_image
            map["png"] = R.drawable.ic_image
            map["gif"] = R.drawable.ic_image
            map["svg"] = R.drawable.ic_image



            map["mp4"] = R.drawable.ic_video
            map["mkv"] = R.drawable.ic_video
            map["mov"] = R.drawable.ic_video
            map["wmv"] = R.drawable.ic_video
            map["flv"] = R.drawable.ic_video
            map["avi"] = R.drawable.ic_video
            map["webm"] = R.drawable.ic_video


            map["pdf"] = R.drawable.ic_pdf
            map["apk"] = R.drawable.ic_android


            map["zip"] = R.drawable.ic_zip
            map["rar"] = R.drawable.ic_zip
            map["tar"] = R.drawable.ic_zip
            map["tar.gz"] = R.drawable.ic_zip
            map["tar.xz"] = R.drawable.ic_zip
            map["7z"] = R.drawable.ic_zip

            map["txt"] = R.drawable.txt_file
            map["bat"] = R.drawable.txt_file
            map["c"] = R.drawable.txt_file
            map["cpp"] = R.drawable.txt_file
            map["java"] = R.drawable.txt_file
            map["py"] = R.drawable.txt_file
            map["js"] = R.drawable.txt_file
            map["json"] = R.drawable.txt_file
            map["html"] = R.drawable.txt_file
            map["php"] = R.drawable.txt_file
            map["css"] = R.drawable.txt_file

            return map
        }
    }
    //endregion MAP ICON PROVIDER

    interface CallbackClick {
        fun onClick(pos: Int)
        fun onClickDp(pos: Int)
        fun onLongClick(pos: Int)
    }

    inner class PersonHolder(val b: RowPersonBinding, clickLis: CallbackClick) :
        RecyclerView.ViewHolder(b.root) {
        init {
            b.root.setOnClickListener {
                clickLis.onClick(bindingAdapterPosition)
            }
            b.imgProfile.setOnClickListener {
                clickLis.onClickDp(bindingAdapterPosition)
            }
            b.root.setOnLongClickListener {
                clickLis.onLongClick(bindingAdapterPosition)
                true
            }
        }
    }

    class PersonDiff : DiffUtil.ItemCallback<PersonModel>() {
        override fun areItemsTheSame(oldItem: PersonModel, newItem: PersonModel) =
            oldItem.phoneNo == newItem.phoneNo

        override fun areContentsTheSame(oldItem: PersonModel, newItem: PersonModel) =
            oldItem.lastMsgId == newItem.lastMsgId && oldItem.isOnline == newItem.isOnline &&
                    oldItem.isSelected == newItem.isSelected && oldItem.unreadCount == newItem.unreadCount
    }


}