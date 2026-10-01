package com.tomer.chitchat.repo

import com.tomer.chitchat.room.MsgMediaType
import java.io.File

interface RepoStorage {

    fun saveBytesToFolder(type: MsgMediaType, fileName: String, data: ByteArray)
    fun getBytesFromFolder(type: MsgMediaType, fileName: String): ByteArray?
    fun getFileFromFolder(type: MsgMediaType, fileName: String): File?
    fun getBytesOfVideoThumb(mediaFileName: String): ByteArray?
    fun getFileOfVideoThumb(mediaFileName: String): File?
    fun saveVideoThumb(mediaFileName: String, data: ByteArray)
    fun isPresent(mediaFileName: String, type: MsgMediaType): Boolean
    fun deleteFile(mediaFileName: String?, msgType: MsgMediaType)

    fun saveDP(phone: String, dpNo: Int, data: ByteArray): File
    suspend fun getDP(phone: String, sync: Boolean = false): File?
    fun deleteDP(phone: String)
}