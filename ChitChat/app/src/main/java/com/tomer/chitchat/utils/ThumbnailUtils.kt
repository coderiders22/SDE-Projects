package com.tomer.chitchat.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun getBmpUsingGlide(any: Any, con: Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            Glide.with(con)
                .asBitmap()
                .load(any)
                .centerInside()
                .override(1600)
                .disallowHardwareConfig()
                .submit()
                .get()
        }.fold(onSuccess = { b -> b }, onFailure = { e -> null })
    }
}

suspend fun getThumbBmpUsingGlide(any: Any, con: Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            Glide.with(con)
                .asBitmap()
                .load(any)
                .centerInside()
                .override(24)
                .disallowHardwareConfig()
                .submit()
                .get()
        }.fold(onSuccess = { b -> b }, onFailure = { e -> null })
    }
}

suspend fun getGifThumbBmpUsingGlide(uri: Uri, con: Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            Glide.with(con)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions().frame(0))
                .override(24)
                .disallowHardwareConfig()
                .submit()
                .get()
        }.fold(onSuccess = { b -> b }, onFailure = { e -> null })
    }
}

suspend fun getGifFrameBmpUsingGlide(uri: Uri, con: Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            Glide.with(con)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions().frame(0))
                .disallowHardwareConfig()
                .submit()
                .get()
        }.fold(onSuccess = { b -> b }, onFailure = { e -> null })
    }
}

fun getAFrameFromVideo(context: Context, uri: Uri, duration: Int): Bitmap? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        return retriever.getFrameAtTime(
            duration * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    } catch (_: Exception) {
        return null
    } finally {
        retriever.release()
    }
}

