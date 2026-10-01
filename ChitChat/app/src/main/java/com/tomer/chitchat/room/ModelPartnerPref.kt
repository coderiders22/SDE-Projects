package com.tomer.chitchat.room

import androidx.annotation.IntRange
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.utils.qrProvider.RenderModel

@Entity(tableName = "partner_prefs")
data class ModelPartnerPref(
    @PrimaryKey
    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "background")
    var background: RenderModel,

    @ColumnInfo(name = "accent")
    var accent: RenderModel,

    @ColumnInfo(name = "backgroundAssetNo")
    var backgroundAssetNo: Int,

    @ColumnInfo(name = "dpNo")
    var dpNo: Int,

    @ColumnInfo(name = "about")
    var about: String,

    @param:IntRange(0, Long.MAX_VALUE)
    @ColumnInfo(name = "lastOnlineMillis")
    val lastOnlineMillis: Long,

    @ColumnInfo(name = "notificationAllowed")
    var notificationAllowed: Boolean,

    @ColumnInfo(name = "chatLocked")
    var chatLocked: Boolean,
)