package com.tomer.chitchat.utils

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ConversionUtils {

    // Converts a long to a Base64 string
    fun toBase64(l: Long): String =
        Base64.getEncoder().encodeToString(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(l).array())

    // Converts a Base64 string to a long
    fun fromBase64(s: String) = ByteBuffer.wrap(Base64.getDecoder().decode(s)).getLong()

    fun byteArrToBase64(arr: ByteArray): String {
        return Base64.getEncoder().encodeToString(arr)
    }

    fun base64ToByteArr(str: String): ByteArray {
        return Base64.getDecoder().decode(str)
    }

    fun convertToWebp(bmp: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.WEBP, 80, baos)
        return baos.toByteArray()
    }

    fun encode(data: String): String {
        return try {
            URLEncoder.encode(data, "UTF-8")
        } catch (e: Exception) {
            data
        }
    }

    fun decode(data: String): String {
        return try {
            URLDecoder.decode(data, "UTF-8")
        } catch (e: Exception) {
            data
        }
    }

    fun millisToTimeText(millis: Long): String {
        val date = Date(millis)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(date)
    }

    fun millisToFullDateText(millis: Long): String {
        val date = Date(millis)
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return format.format(date)
    }

    fun getRelativeTime(millis: Long): String {
        val providedCal = Calendar.getInstance()
        providedCal.timeInMillis = millis
        val today = Calendar.getInstance()

        val daysGap = today.get(Calendar.DAY_OF_YEAR) - providedCal.get(Calendar.DAY_OF_YEAR)

        if (daysGap == 0)
            return millisToTimeText(millis)
        else if (daysGap == 1) return "Yesterday"
        else {
            val date = Date(millis)
            val format = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            return format.format(date)
        }
    }

    val mimeTypes by lazy {
        mapOf(
            // Images
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "bmp" to "image/bmp",
            "webp" to "image/webp",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",

            // Videos
            "mp4" to "video/mp4",
            "avi" to "video/x-msvideo",
            "mov" to "video/quicktime",
            "wmv" to "video/x-ms-wmv",
            "mkv" to "video/x-matroska",
            "flv" to "video/x-flv",
            "webm" to "video/webm",
            "3gp" to "video/3gpp",

            // Audio
            "mp3" to "audio/mpeg",
            "wav" to "audio/wav",
            "flac" to "audio/flac",
            "ogg" to "audio/ogg",
            "m4a" to "audio/mp4",
            "aac" to "audio/aac",
            "wma" to "audio/x-ms-wma",

            // Documents
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "txt" to "text/plain",
            "rtf" to "application/rtf",
            "csv" to "text/csv",
            "xml" to "application/xml",
            "json" to "application/json",
            "html" to "text/html",
            "htm" to "text/html",
            "md" to "text/markdown",

            // Archives
            "zip" to "application/zip",
            "rar" to "application/x-rar-compressed",
            "tar" to "application/x-tar",
            "gz" to "application/gzip",
            "7z" to "application/x-7z-compressed",
            "bz2" to "application/x-bzip2",

            // Applications
            "exe" to "application/vnd.microsoft.portable-executable",
            "apk" to "application/vnd.android.package-archive",
            "jar" to "application/java-archive",
            "dmg" to "application/x-apple-diskimage",
            "iso" to "application/x-iso9660-image",

            // Fonts
            "ttf" to "font/ttf",
            "otf" to "font/otf",
            "woff" to "font/woff",
            "woff2" to "font/woff2"
        ) //"application/octet-stream"
    }
}
