package com.tomer.chitchat.crypto

import com.tomer.chitchat.room.Dao

class RoomCipherRepo(
    private val doa: Dao
) : RepoCipher {
    override fun getKey(phone: String): CryptoKey? =
        doa.getKey(phone).getOrNull(0)


    override fun saveKey(key: CryptoKey) {
        doa.insertKey(key)
    }
}