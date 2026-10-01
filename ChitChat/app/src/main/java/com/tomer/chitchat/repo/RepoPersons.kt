package com.tomer.chitchat.repo

import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.ModelRoomPersons

interface RepoPersons {

    suspend fun getAllPersons():List<ModelRoomPersons>
    suspend fun insertPerson(personRoom: ModelRoomPersons)
    suspend fun deletePersonById(phoneNo: String)
    suspend fun updatePerson(personRoom: ModelRoomPersons)
    suspend fun getPersonByPhone(phoneNo:String) : ModelRoomPersons?


    fun getPersonPref(phoneNo: String) : ModelPartnerPref?
    suspend fun insertPersonPref(model: ModelPartnerPref)

}