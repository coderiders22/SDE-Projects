package com.tomer.chitchat.repo

import com.tomer.chitchat.room.ModalMediaUpload

interface RepoMedia {

    fun saveMedia(media: ModalMediaUpload)

    fun getUriFromFileName(fileName: String): String?

    fun getFileNameFromUri(uri: String): String?

}