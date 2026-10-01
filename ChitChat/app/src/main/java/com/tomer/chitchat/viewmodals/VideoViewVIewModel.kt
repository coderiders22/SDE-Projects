package com.tomer.chitchat.viewmodals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.getAFrameFromVideo
import com.tomer.chitchat.utils.timeTextFromMs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoViewVIewModel @Inject constructor(
    @param:ApplicationContext val appContext: Context,
    private val repo: RepoPersons,
    private val repoStorage: RepoStorage,
) : ViewModel() {
    val exoPlayer = ExoPlayer.Builder(appContext).build()
    private var seekBarSyncJob = viewModelScope.launch { }

    val partnerName = MutableStateFlow<ModelPartnerPref?>(null)
    val showPlayButton = MutableStateFlow(true)
    val isMute = MutableStateFlow(false)
    val seekBar = MutableStateFlow(0f)
    val times = MutableStateFlow(Pair("", ""))
    val bmp = MutableStateFlow<Bitmap?>(null)
    val isImgVisible = MutableStateFlow(true)

    private var videoName: String? = null
    var videoFile: File? = null
    var thumbBmp: Bitmap? = null


    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    seekBarSyncJob.cancel()
                    seekBarSyncJob = createSeekBarSyncJob()
                } else seekBarSyncJob.cancel()
                viewModelScope.launch {
                    showPlayButton.emit(!isPlaying)
                }
            }
        })
    }

    fun onEnd() {
        Log.d("TAG--", "onEnd: ")
        viewModelScope.launch {
            isImgVisible.emit(false)
            bmp.emit(null)
        }
    }

    fun processSelectedUri(videoName: String, partnerPhone: String, videoTime: String) {
        if (this.videoName != null) return
        this.videoName = videoName
        val vidFile = repoStorage.getFileFromFolder(MsgMediaType.VIDEO, videoName) ?: return
        videoFile = vidFile
        val vidThumb = repoStorage.getFileOfVideoThumb(videoName)
        if (vidThumb == null)
            getAFrameFromVideo(appContext, Uri.fromFile(vidFile), 200)
                ?.let {
                    viewModelScope.launch {
                        val boos = ByteArrayOutputStream()
                        bmp.emit(it)
                        it.compress(Bitmap.CompressFormat.WEBP, 80, boos)
                        repoStorage.saveVideoThumb(videoName, boos.toByteArray())
                        thumbBmp = it
                    }
                }
        else viewModelScope.launch {
            bmp.emit(
                BitmapFactory.decodeFile(vidThumb.absolutePath)
                    .also { thumbBmp = it })
        }
        viewModelScope.launch {
            val p = repo.getPersonPref(phoneNo = partnerPhone)
            partnerName.emit(p)
        }
        viewModelScope.launch {
            times.emit("0:00" to videoTime)
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(vidFile)))
            exoPlayer.prepare()
        }
    }

    private fun createSeekBarSyncJob(): Job {
        return viewModelScope.launch {
            while (isActive) {
                val currentPosition = exoPlayer.currentPosition
                val totalDuration = exoPlayer.duration

                if (totalDuration > 0) {
                    val progress = currentPosition.toFloat() / totalDuration
                    seekBar.emit(progress)
                    times.emit(currentPosition.timeTextFromMs() to times.value.second)
                }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.pause()
        exoPlayer.release()
        seekBarSyncJob.cancel()
        Log.d("TAG--", "onCleared: ")
    }
}