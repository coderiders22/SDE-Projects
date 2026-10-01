package com.tomer.chitchat.modals.prefs

import androidx.annotation.FloatRange
import com.google.gson.annotations.SerializedName

data class MyPrefs(
    @SerializedName("phone")
    var phone: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("about")
    var about: String,
    @SerializedName("msgItemCorners")
    var msgItemCorners: Float,
    @SerializedName("textSize")
    var textSize: Float,
    @SerializedName("dpNo")
    var dpNo: Int,
    @SerializedName("parallaxFactor")
    @param:FloatRange(0.0, 8.2)
    var parallaxFactor: Float
) {
    fun copyFrom(pref: MyPrefs) {
        this.name = pref.name
        this.about = pref.about
        this.msgItemCorners = pref.msgItemCorners
        this.textSize = pref.textSize
        this.dpNo = pref.dpNo
    }
}
