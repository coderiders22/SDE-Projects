package com.tomer.chitchat.repo

import com.tomer.chitchat.room.ModelRoomPersonRelation

interface RepoRelations {
    fun saveRelation(modelRoomPersonRelation: ModelRoomPersonRelation)
    fun getRelation(phoneNO:String) : ModelRoomPersonRelation?
}