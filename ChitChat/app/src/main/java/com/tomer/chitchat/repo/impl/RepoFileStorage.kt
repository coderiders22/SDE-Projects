package com.tomer.chitchat.repo.impl

import android.content.Context
import android.os.Build
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.NetworkUtils
import com.tomer.chitchat.utils.Utils.Companion.getDpLink
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class RepoFileStorage @Inject constructor(
    context: Context,
    private val room: Dao
) : RepoStorage {


    private val folder =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            File(
                File(context.getExternalFilesDir("ChitChat"), "media")
                    .absolutePath.replace("Android/data", "Android/media")
                    .replace(".chitchat/files", ".chitchat")
            )
        else File(context.getExternalFilesDir("ChitChat"), "media")

    private val dpFolder = File(folder.parentFile, "profile").apply {
        if (this.mkdirs())
            File(this, ".nomedia").createNewFile()
    }

    override fun saveBytesToFolder(type: MsgMediaType, fileName: String, data: ByteArray) {
        val folderChild = File(folder, "/$type")
        if (folderChild.mkdirs()) File(folderChild, ".nomedia").createNewFile()
        val file = File(folderChild, fileName)
        FileOutputStream(file).use {
            it.write(data)
        }
    }

    override fun saveVideoThumb(mediaFileName: String, data: ByteArray) {
        val folderChild = File(folder, "/VideoThumbs")
        if (folderChild.mkdirs()) File(folderChild, ".nomedia").createNewFile()
        val file = File(folderChild, "$mediaFileName.webp")
        FileOutputStream(file).use {
            it.write(data)
        }
    }

    override fun isPresent(mediaFileName: String, type: MsgMediaType): Boolean {
        val folderChild = File(folder, "/$type")
        val file = File(folderChild, mediaFileName)
        return file.exists()
    }

    override fun deleteFile(mediaFileName: String?, msgType: MsgMediaType) {
        if (mediaFileName == null) return
        val file = File(folder, "/$msgType/$mediaFileName")
        if (file.exists()) file.delete()
    }

    override fun getBytesFromFolder(type: MsgMediaType, fileName: String): ByteArray? {
        val file = File(folder, "/$type/$fileName")
        if (!file.exists()) return null
        FileInputStream(file).use {
            return it.readBytes()
        }
    }

    override fun getFileFromFolder(type: MsgMediaType, fileName: String): File? {
        val file = File(folder, "/$type/$fileName")
        return if (!file.exists()) null
        else file
    }

    override fun getBytesOfVideoThumb(mediaFileName: String): ByteArray? {
        val file = File(folder, "/VideoThumbs/$mediaFileName.webp")
        if (!file.exists()) return null
        FileInputStream(file).use {
            return it.readBytes()
        }
    }

    override fun getFileOfVideoThumb(mediaFileName: String): File? {
        val file = File(folder, "/VideoThumbs/$mediaFileName.webp")
        if (!file.exists()) return null
        return file
    }

    override fun saveDP(phone: String, dpNo: Int, data: ByteArray): File {
        val file = File(dpFolder, "$phone-$dpNo")
        FileOutputStream(file).use {
            it.write(data)
        }
        return file
    }

    override suspend fun getDP(phone: String, sync: Boolean): File? {
        val files = dpFolder.listFiles { it.name.startsWith(phone) }
        val file = files?.lastOrNull()
        if (sync)
            return file.takeIf { it?.exists() == true }
        val dpNO = room.getPersonPref(phone).firstOrNull()?.dpNo ?: 1
        val fileA = file.takeIf { it?.exists() == true } ?: File(dpFolder, "$phone-$dpNO")
        if (fileA.exists()) return file
        return if (NetworkUtils.downloadBytesToFile(phone.getDpLink(), fileA))
            fileA else null
    }

    override fun deleteDP(phone: String) {
        val files = dpFolder.listFiles { it.name.startsWith(phone) }
        files?.lastOrNull()?.delete()
    }

}