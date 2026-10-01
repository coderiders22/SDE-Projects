package com.tomer.chitchat.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tomer.chitchat.crypto.CryptoKey
import com.tomer.chitchat.modals.prefs.RenderConvertor

@Database(
    entities = [
        ModelRoomMessage::class,
        ModelRoomPersons::class,
        CryptoKey::class,
        ModelRoomPersonRelation::class,
        ModalMediaUpload::class,
        ModelPartnerPref::class,
        ModelRoomCall::class,
    ], version = 1, exportSchema = false
)
@TypeConverters(RenderConvertor::class)
abstract class Database : RoomDatabase() {
    abstract fun messageDao(): Dao
}