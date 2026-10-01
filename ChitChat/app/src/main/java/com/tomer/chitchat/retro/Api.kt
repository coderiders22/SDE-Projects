package com.tomer.chitchat.retro

import com.tomer.chitchat.retro.modals.LoginResponse
import com.tomer.chitchat.retro.modals.SyncResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface Api {

    @FormUrlEncoded
    @POST("/login")
    suspend fun getLoginToken(
        @Field("otp") otp: String,
        @Field("notiToken") notiToken: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("/login/send-otp")
    suspend fun sendOtp(
        @Field("phone") phone: String,
    ): Response<String>


    @FormUrlEncoded
    @POST("/update/name")
    suspend fun updateName(
        @Field("name") name: String
    ): Response<String>

    @FormUrlEncoded
    @POST("/update/firebase")
    suspend fun updateNotificationToken(
        @Field("notiId") name: String
    ): Response<String>

    @FormUrlEncoded
    @POST("/update/about")
    suspend fun updateAbout(
        @Field("about") name: String
    ): Response<String>

    @FormUrlEncoded
    @POST("/chat")
    suspend fun sendAck(
        @Field("messageId") msgId: String,
        @Field("toPhone") partnerNo: String,
    ): Response<String>

    @FormUrlEncoded
    @POST("/chat")
    suspend fun sendAckBulk(
        @Field("messageIds") msgIds: String,
        @Field("toPhone") partnerNo: String,
    ): Response<String>

    @FormUrlEncoded
    @POST("/chat/check")
    suspend fun checkForUpload(
        @Field("uri") uri: String
    ): Response<String>

    @Multipart
    @POST("/update/uploadImage")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): Response<String>


    @Multipart
    @POST("/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("type") type: String,
        @Part("uri") uri: String
    ): Response<String>

    @FormUrlEncoded
    @POST("/chat/name")
    suspend fun getName(@Field("phone") phone: String): Response<String>

    @FormUrlEncoded
    @POST("/sync")
    suspend fun getSyncedData(@Field("phoneNos") phoneNos: String): Response<SyncResponse>
}