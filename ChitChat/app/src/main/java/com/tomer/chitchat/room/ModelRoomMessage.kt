package com.tomer.chitchat.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tomer.chitchat.modals.states.MsgStatus

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["partnerId", "timeMillis"])
    ]
)
data class ModelRoomMessage(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "replyId")
    val replyId: Long,

    @ColumnInfo(name = "partnerId")
    val partnerId: String,

    @ColumnInfo(name = "msgText")
    var msgText: String,

    @ColumnInfo(name = "repText")
    val repText: String,

    @ColumnInfo(name = "msgStatus")
    var msgStatus: MsgStatus,

    @ColumnInfo(name = "msgType")
    val msgType: MsgMediaType,

    @ColumnInfo(name = "replyType")
    val replyType: MsgMediaType,

    @ColumnInfo(name = "mediaFileName")
    var mediaFileName: String?,

    @ColumnInfo(name = "replyMediaFileName")
    val replyMediaFileName: String?,

    @ColumnInfo(name = "mediaSize")
    val mediaSize: String,

    @ColumnInfo(name = "aspectRatio")
    val aspectRatio: Float?,

    //used to store some arbitrary info used in any cases
    @ColumnInfo(name = "info")
    val info: String,

    @ColumnInfo(name = "isSent")
    val isSent: Boolean,

    @ColumnInfo(name = "isRep")
    val isRep: Boolean,

    @ColumnInfo(name = "timeMillis")
    val timeMillis: Long
)