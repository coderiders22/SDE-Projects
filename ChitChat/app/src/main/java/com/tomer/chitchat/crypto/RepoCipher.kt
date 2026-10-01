package com.tomer.chitchat.crypto

interface RepoCipher {
    fun getKey(phone: String): CryptoKey?

    fun saveKey(key: CryptoKey)
}