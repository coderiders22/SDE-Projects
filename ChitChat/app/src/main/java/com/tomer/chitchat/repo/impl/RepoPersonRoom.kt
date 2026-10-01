package com.tomer.chitchat.repo.impl

import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.ModelRoomPersons

class RepoPersonRoom(
    private val room: Dao
) : RepoPersons {
    override suspend fun getAllPersons(): List<ModelRoomPersons> {
        return room.getMainViewList()
    }

    override suspend fun insertPerson(personRoom: ModelRoomPersons) {
        room.insertPerson(personRoom)
    }

    override suspend fun deletePersonById(phoneNo: String) {
        room.deletePerson(phoneNo)
    }

    override suspend fun updatePerson(personRoom: ModelRoomPersons) {
        room.deletePerson(personRoom.phoneNo)
        room.insertPerson(personRoom)
    }

    override suspend fun getPersonByPhone(phoneNo: String): ModelRoomPersons? =
        room.getPerson(phoneNo)

    override fun getPersonPref(phoneNo: String) = room.getPersonPref(phoneNo).firstOrNull()

    override suspend fun insertPersonPref(model: ModelPartnerPref) = room.insertPartnerPrefs(model)
}