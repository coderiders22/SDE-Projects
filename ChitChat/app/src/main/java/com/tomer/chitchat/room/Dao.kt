package com.tomer.chitchat.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tomer.chitchat.crypto.CryptoKey

@Dao
interface Dao {

    //region TABLE MESSAGES


    @Query("update messages set id=:newId  where id=:tempId")
    fun updateMsgSent(tempId: Long, newId: Long)

    @Query("update messages set msgStatus=:status  where id=:id")
    fun updateMsgReceived(id: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMsg(msg: ModelRoomMessage)

    @Query("delete from messages where id=:id")
    fun deleteFromId(id: Long)

    @Query("select * from messages where id=:id")
    fun getFromID(id: Long): ModelRoomMessage?

    @Query("select * from messages where partnerId=:partnerId AND timeMillis < :time order by timeMillis DESC LIMIT :limit")
    fun getMsgBeforeByUser(
        partnerId: String, limit: Int,
        time: Long = System.currentTimeMillis()
    ): List<ModelRoomMessage>

    @Query("select * from messages where (msgType == 'IMAGE' OR msgType == 'GIF' OR msgType == 'VIDEO') AND partnerId=:partnerId order by timeMillis DESC")
    fun getMsgsOfUserOnlyMedia(partnerId: String) : List<ModelRoomMessage>

    @Query("select * from messages where id=:msgId")
    fun getByUser(msgId: Long): List<ModelRoomMessage>

    @Query("select * from messages where mediaFileName=:fileName")
    fun getByFileName(fileName: String): List<ModelRoomMessage>

    @Query("DELETE from messages where id=:id")
    fun deleteById(id: Long)

    @Query("DELETE from messages where partnerId=:no")
    fun deleteAllByPhone(no: String)

    @Query("select * from messages where partnerId=:phoneNo order by timeMillis DESC LIMIT 1")
    fun getLastOfUser(phoneNo: String): List<ModelRoomMessage>


    //endregion TABLE MESSAGES

    //region TABLE PERSONS

    @Query("select * from persons order by timeMillis DESC")
    fun getMainViewList(): List<ModelRoomPersons>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPerson(person: ModelRoomPersons)

    @Query("delete from persons where phoneNo=:phoneNo")
    fun deletePerson(phoneNo: String)

    @Query("select * from persons where phoneNo=:phoneNo")
    fun getPerson(phoneNo: String): ModelRoomPersons?

    //endregion TABLE PERSONS

    //region TABLE KEYS

    @Query("select * from keystore where partnerUserPhone=:phoneNo")
    fun getKey(phoneNo: String): List<CryptoKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertKey(person: CryptoKey)

    @Query("delete from keystore where partnerUserPhone=:phoneNo")
    fun deleteKey(phoneNo: String)

    //endregion TABLE KEYS

    // region Relations

    @Query("select * from relation where partnerId=:phoneNo")
    fun getRelation(phoneNo: String): List<ModelRoomPersonRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRelation(person: ModelRoomPersonRelation)

    //endregion Relations

    //region TABLE MEDIA

    @Query("select * from media where uri=:uri")
    fun getMediaNameOfUri(uri: String): List<ModalMediaUpload>

    @Query("select * from media where localFileName=:fileName")
    fun getUriOfMediaName(fileName: String): List<ModalMediaUpload>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMedia(media: ModalMediaUpload)

    //endregion TABLE MEDIA

    //region PARTNER_PREFS

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPartnerPrefs(personPref: ModelPartnerPref)

    @Query("select * from partner_prefs where phone=:phoneNo")
    fun getPersonPref(phoneNo: String): List<ModelPartnerPref>

    //endregion PARTNER_PREFS
}
