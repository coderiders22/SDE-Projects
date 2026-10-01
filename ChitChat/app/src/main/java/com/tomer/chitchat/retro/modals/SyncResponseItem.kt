package com.tomer.chitchat.retro.modals

import com.google.gson.annotations.SerializedName

data class SyncResponseItem(
    @SerializedName("about")
    val about: String,
    @SerializedName("dpNo")
    val dpNo: Int,
    @SerializedName("phone")
    val phone: String
)