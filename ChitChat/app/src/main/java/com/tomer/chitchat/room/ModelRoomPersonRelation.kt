package com.tomer.chitchat.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relation")
data class ModelRoomPersonRelation(
    @PrimaryKey
    @ColumnInfo(name = "partnerId")
    val partnerId: String,

    @ColumnInfo(name = "isConnSent")
    var isConnSent: Boolean,

    @ColumnInfo(name = "isAccepted")
    var isAccepted: Boolean,

    @ColumnInfo(name = "isRejected")
    var isRejected: Boolean
)