package com.tomer.chitchat.crypto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keystore")
data class CryptoKey(
    @PrimaryKey
    val partnerUserPhone: String,
    val keyStatus: Boolean, //false if tempKey only
    val tempKeyMy: String,
    val keyBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CryptoKey) return false

        if (partnerUserPhone != other.partnerUserPhone) return false
        if (keyStatus != other.keyStatus) return false
        if (!keyBytes.contentEquals(other.keyBytes)) return false
        if (tempKeyMy != other.tempKeyMy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = partnerUserPhone.hashCode()
        result = 31 * result + keyStatus.hashCode()
        result = 31 * result + keyBytes.contentHashCode()
        result = 31 * result + tempKeyMy.hashCode()
        return result
    }
}