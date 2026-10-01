package com.tomer.chitchat.retro.modals

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("isAlreadyRegistered")
    val isAlreadyRegistered: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("about")
    val about: String,
    @SerializedName("token")
    val token: String
)