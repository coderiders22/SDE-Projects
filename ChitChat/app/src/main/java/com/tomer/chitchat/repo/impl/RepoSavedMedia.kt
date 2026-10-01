package com.tomer.chitchat.repo.impl

import com.tomer.chitchat.repo.RepoMedia
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.ModalMediaUpload

class RepoSavedMedia(
    private val dao: Dao
) : RepoMedia {
    override fun saveMedia(media: ModalMediaUpload) {
        dao.insertMedia(media)
    }

    override fun getUriFromFileName(fileName: String): String? {
        val l = dao.getUriOfMediaName(fileName)
        if (l.isEmpty()) return null
        return l[0].uri
    }

    override fun getFileNameFromUri(uri: String): String? {
        val l = dao.getMediaNameOfUri(uri)
        if (l.isEmpty()) return null
        return l[0].localFileName
    }
}