package com.tomer.chitchat.adap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.tomer.chitchat.databinding.RowEmojiBinding

class EmojiAdapter(private val clickLis: (Int) -> Unit) : ListAdapter<String, EmojiAdapter.EmojiHolder>(StringUtils()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiHolder =
        EmojiHolder(RowEmojiBinding.inflate(LayoutInflater.from(parent.context),parent,false), clickLis)


    override fun onBindViewHolder(holder: EmojiHolder, position: Int) {
        holder.b.emoTv.text = getItem(position)
    }

    inner class EmojiHolder(val b: RowEmojiBinding, clickLis: (Int) -> Unit) : ViewHolder(b.root){
        init {
            b.root.setOnClickListener { clickLis(bindingAdapterPosition) }
        }
    }

    class StringUtils : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}