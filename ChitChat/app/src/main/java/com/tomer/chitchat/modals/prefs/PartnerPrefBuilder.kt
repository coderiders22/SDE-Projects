package com.tomer.chitchat.modals.prefs

import android.graphics.Color
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.utils.qrProvider.AssetsProvider
import com.tomer.chitchat.utils.qrProvider.RenderModel
import androidx.core.graphics.toColorInt

class PartnerPrefBuilder(private val phone: String, private var name: String = "") {
    private var background: RenderModel = RenderModel(1f, Color.DKGRAY, AssetsProvider.gradType.getOrDefault(4, null))
    private var accent: RenderModel = RenderModel(1f, "#005FEB".toColorInt())
    private var backgroundAsset: Int = 7
    private var dpNo: Int = 1
    private var about: String = ""
    private var lastOnlineMillis: Long = System.currentTimeMillis()
    private var notificationAllowed: Boolean = true
    private var chatLocked: Boolean = false

    fun build(): ModelPartnerPref {
        return ModelPartnerPref(
            phone,
            name,
            background,
            accent,
            backgroundAsset,
            dpNo,
            about,
            lastOnlineMillis,
            notificationAllowed,
            chatLocked
        )
    }
}