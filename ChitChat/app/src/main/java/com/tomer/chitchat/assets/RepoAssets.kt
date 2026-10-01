package com.tomer.chitchat.assets

import com.tomer.chitchat.modals.states.UiMsgModal
import java.io.File

interface RepoAssets {

    suspend fun getLottieJson(name: String,sync:Boolean = false): String?
    suspend fun getGoogleLottieJson(nameJson: String,sync:Boolean = false): String?

    suspend fun getGifFile(name: String,sync:Boolean = false): File?
    suspend fun getGifTelemoji(name: String,sync:Boolean = false): File?

    suspend fun getRandomJson(): UiMsgModal

}