package com.tomer.chitchat.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.resize.AtMostResizer
import com.otaliastudios.transcoder.source.TrimDataSource
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.RemoveTrackStrategy
import kotlin.concurrent.thread

data class VideoDetails(
    val durationMs: Long?,
    val name: String,
    val width: Int?,
    val height: Int?,
    val rotation: Int?,
    val title: String?,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
)

fun getAllPossibleDetails(context: Context, videoUri: Uri): VideoDetails? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, videoUri)

        // Helper function to safely extract metadata and convert it
        fun <T> extract(key: Int, transform: (String) -> T): T? {
            return try {
                retriever.extractMetadata(key)?.let(transform)
            } catch (_: Exception) {
                null
            }
        }

        // Standard metadata keys
        val width = extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH, String::toInt)
        val height = extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT, String::toInt)
        val rotation = extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION, String::toInt)

        val fileName = getFileName(videoUri, context.contentResolver)

        return VideoDetails(
            name = fileName ?: "",
            durationMs = extract(MediaMetadataRetriever.METADATA_KEY_DURATION, String::toLong),
            width = width,
            height = height,
            rotation = rotation,
            title = extract(MediaMetadataRetriever.METADATA_KEY_TITLE) { it },
            hasVideo = extract(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) { it } == "yes",
            hasAudio = extract(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) { it } == "yes",
        )

    } catch (e: Exception) {
        // Log the exception or handle it as needed
        Log.e("VideoDetails", "Failed to process video URI: $videoUri", e)
        return null
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            Log.e("VideoDetails", "Error releasing MediaMetadataRetriever", e)
        }
    }
}

private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
    var fileName: String? = null
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

fun extract10Frames(videoUri: Uri, con: Context, frameCallback: (Int, Bitmap) -> Unit) {
    thread {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(con, videoUri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                .toLong()
            val count = 10

            for (i in 0 until count) {
                runCatching {
                    val timeUs = (i * durationMs / count) * 1000
                    retriever.getScaledFrameAtTime(
                        timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        100, 200
                    )?.let { frameCallback(i, it) }
                }
            }
        } catch (_: Exception) {
        } finally {
            retriever.release()
        }
    }
}

fun compressAndTrimDeepMedia(
    context: Context,
    inputUri: Uri,
    outputPath: String,
    transCoderLis: TranscoderListener,
    witAudio: Boolean,
    startTimeMilli: Long = -1L,
    endTimeMilli: Long = -1L
) {

    // 1. Convert to Microseconds for trimming
    // If user passes -1, we use a Safe Long value or just logic later
    val startMicro = if (startTimeMilli != -1L) (startTimeMilli * 1_000) else 0L
    val endMicro = if (endTimeMilli != -1L) (endTimeMilli * 1_000) else -1L
    Log.d(
        "TAG--",
        "compressAndTrimDeepMedia: ${(endMicro - startMicro).div(1000)} Audio: $witAudio"
    )

    // 2. Define Video Compression Strategy
    // NOTE: Switched to HEVC (H.265) if you want minimum bytes. 
    // If you need maximum compatibility, keep MIMETYPE_VIDEO_AVC.
    val videoStrategy = DefaultVideoStrategy.Builder()
        .addResizer(AtMostResizer(720)) // Ensures max dimension is 720p (720x1280 or 1280x720)
        .bitRate(1_000_000)      // 1 Mbps
        .frameRate(24)           // 24 FPS
        .keyFrameInterval(2.0f)  // Keyframe every 3s
        .mimeType(MediaFormat.MIMETYPE_VIDEO_AVC) // H.264
        .build()

    // 3. Define Audio Strategy
    val audioStrategy = if (witAudio) {
        DefaultAudioStrategy.Builder()
            .channels(2)
            .bitRate(128_000) // 128kbps standard quality
            .sampleRate(48_000)
            .build()
    } else {
        // This explicitly removes the audio track from the output
        RemoveTrackStrategy()
    }

    // 4. Create Source (Trimmed or Full)
    val originalSource = UriDataSource(context, inputUri)
    val source = if (endMicro != -1L && startMicro >= 0)
        TrimDataSource(originalSource, startMicro, endMicro)
    else originalSource

    // 5. Run Transcoder
    Transcoder.into(outputPath)
        .addDataSource(source)
        .setVideoTrackStrategy(videoStrategy)
        .setAudioTrackStrategy(audioStrategy)
        .setListener(transCoderLis)
        .transcode()
}

fun estimateFileSize(
    startTimeMs: Long,
    endTimeMs: Long,
    withAudio: Boolean
): String {
    // 1. Calculate Duration in Seconds (Floating point for precision)
    // If end is 0 or less, we return 0 size
    if (endTimeMs <= startTimeMs) return "0 MB"

    val durationSec = (endTimeMs - startTimeMs) / 1000.0

    // 2. Bitrates (Must match your Transcoder settings)
    val videoBitrateBits = 2_500_000L // 2.4 Mbps
    val audioBitrateBits = if (withAudio) 128_000L else 0L

    // 3. Total Bits per Second
    val totalBitrateBits = (videoBitrateBits + audioBitrateBits).times(1.1f)

    // 4. Calculate Total Bytes
    // Formula: (BitsPerSec * Seconds) / 8 = Bytes
    val totalSizeBytes = (totalBitrateBits * durationSec) / 8

    // 5. Convert to Megabytes (MB)
    val sizeInMb = totalSizeBytes / (1024 * 1024)

    return "%.2f MB".format(sizeInMb)
}