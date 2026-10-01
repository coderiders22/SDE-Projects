package com.tomer.chitchat.repo.impl

import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.ModelRoomMessage
import javax.inject.Inject

class RepoMessagesImpl @Inject constructor(
    private val dao: Dao
) : RepoMessages {

    override suspend fun addMsg(modal: ModelRoomMessage) {
        dao.insertMsg(modal)
    }

    override suspend fun deleteById(msgId: Long) {
        dao.deleteById(msgId)
    }

    override suspend fun deleteAllByUser(phone: String) {
        dao.deleteAllByPhone(phone)
    }

    override suspend fun getMsg(id: Long): ModelRoomMessage? = dao.getByUser(id).getOrNull(0)
    override suspend fun getMsgsOfUser(
        partnerId: String, timeBefore: Long, limit: Int
    ): List<ModelRoomMessage> = dao.getMsgBeforeByUser(partnerId, limit,timeBefore)

    override suspend fun getMsgsOfUserOnlyMedia(partnerId: String): List<ModelRoomMessage>
    = dao.getMsgsOfUserOnlyMedia(partnerId)

    override suspend fun updateMsg(tempId: Long, newId: Long) {
        dao.updateMsgSent(tempId, newId)
    }

    override suspend fun updateMsgReceived(msgId: Long, status: MsgStatus) {
        dao.updateMsgReceived(msgId, status.name)
    }

    override suspend fun getMsgFromFileName(fileName: String) =
        dao.getByFileName(fileName).getOrNull(0)

    override suspend fun getLastMsgForPartner(phone: String) = dao.getLastOfUser(phone).getOrNull(0)
}