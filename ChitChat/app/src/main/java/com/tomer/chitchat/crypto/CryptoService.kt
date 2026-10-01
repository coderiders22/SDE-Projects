package com.tomer.chitchat.crypto

import java.io.InputStream
import java.io.OutputStream

interface CryptoService {
    fun setCurrentPartner(phone: String): Boolean

    fun updateKeyAndGenerateFullKey(secret: String,phone: String) : Boolean
    fun checkForKeyAndGenerateIfNot(phone: String):CryptoKey

    fun encString(data: String): String?
    fun decString(phone:String ,data: String): String?

    fun encStream(ins: InputStream, ois: OutputStream) : Boolean
    fun decStream(phone:String ,ins: InputStream, ois: OutputStream) : Boolean
}