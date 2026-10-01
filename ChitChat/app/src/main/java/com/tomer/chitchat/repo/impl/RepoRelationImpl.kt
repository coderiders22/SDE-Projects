package com.tomer.chitchat.repo.impl

import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.ModelRoomPersonRelation

class RepoRelationImpl(
    private val dao: Dao
) : RepoRelations {
    override fun saveRelation(modelRoomPersonRelation: ModelRoomPersonRelation) {
        dao.insertRelation(modelRoomPersonRelation)
    }

    override fun getRelation(phoneNO: String) =
        dao.getRelation(phoneNO).getOrNull(0)

}