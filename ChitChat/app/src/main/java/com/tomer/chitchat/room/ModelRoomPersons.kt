package com.tomer.chitchat.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.modals.states.MsgStatus

@Entity(tableName = "persons")
data class ModelRoomPersons(
    @PrimaryKey
    @ColumnInfo(name = "phoneNo")
    val phoneNo: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "mediaType")
    val mediaType: MsgMediaType,

    @ColumnInfo(name = "lastMsg")
    val lastMsg: String,

    @ColumnInfo(name = "lastMsgId")
    val lastMsgId: Long,

    @ColumnInfo(name = "timeMillis")
    var timeMillis: Long,

    @ColumnInfo(name = "unReadCount")
    var unReadCount: Int = 0,

    @ColumnInfo(name = "lastSeenMillis")
    var lastSeenMillis: Long,

    @ColumnInfo(name = "isSent")
    val isSent: Boolean,

    @ColumnInfo(name = "msgStatus")
    var msgStatus: MsgStatus
)