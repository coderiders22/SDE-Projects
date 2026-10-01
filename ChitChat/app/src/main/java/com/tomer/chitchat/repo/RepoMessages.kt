package com.tomer.chitchat.repo

import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.room.ModelRoomMessage

interface RepoMessages {

    suspend fun addMsg(modal: ModelRoomMessage)
    suspend fun deleteById(msgId: Long)
    suspend fun deleteAllByUser(phone: String)
    suspend fun getMsg(id: Long): ModelRoomMessage?
    suspend fun getMsgsOfUser(partnerId: String,timeBefore: Long,limit: Int = 20): List<ModelRoomMessage>
    suspend fun getMsgsOfUserOnlyMedia(partnerId: String): List<ModelRoomMessage>
    suspend fun updateMsg(tempId: Long, newId: Long)
    suspend fun updateMsgReceived(msgId: Long, status: MsgStatus)
    suspend fun getMsgFromFileName(fileName: String): ModelRoomMessage?
    suspend fun getLastMsgForPartner(phone: String): ModelRoomMessage?
}