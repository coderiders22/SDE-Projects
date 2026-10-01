package com.tomer.chitchat.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.tomer.chitchat.assets.RepoAssets
import com.tomer.chitchat.assets.WebAssetsRepo
import com.tomer.chitchat.crypto.CryptoCipher
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.crypto.RepoCipher
import com.tomer.chitchat.crypto.RoomCipherRepo
import com.tomer.chitchat.notifications.AndroidNotificationService
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMedia
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.repo.impl.RepoFileStorage
import com.tomer.chitchat.repo.impl.RepoMessagesImpl
import com.tomer.chitchat.repo.impl.RepoPersonRoom
import com.tomer.chitchat.repo.impl.RepoRelationImpl
import com.tomer.chitchat.repo.impl.RepoSavedMedia
import com.tomer.chitchat.repo.impl.RepoUtilImpl
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.retro.SyncCallAdapterFactory
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.Database
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.WebSocketHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class HiltModules {

    @Provides
    @Singleton
    fun provideRetroApiCompiler(repoUtils: RepoUtils): Api {
        return Retrofit.Builder().baseUrl(Utils.SERVER_LINK)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(SyncCallAdapterFactory.create())
            .client(
                OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer ${repoUtils.getToken()}")
                        .build()
                    try {
                        chain.proceed(authenticatedRequest)
                    } catch (e: Exception) {
                        Response.Builder().request(chain.request())
                            .protocol(Protocol.HTTP_1_0)
                            .message(e.message.toString())
                            .code(400).body(ResponseBody.Companion.create(null, "")).build()
                    }
                }
                .build())
            .build()
            .create(Api::class.java)
    }

    @Provides
    @Singleton
    fun provideGlobalWebSocket(
        repoMsgs: RepoMessages,
        repoStorage: RepoStorage,
        repoPersons: RepoPersons,
        gson: Gson,
        notificationService: NotificationService,
        repoRelations: RepoRelations,
        cryptoService: CryptoService,
    ): WebSocketHandler {
        return WebSocketHandler(
            repoMsgs = repoMsgs,
            repoStorage = repoStorage,
            repoPersons = repoPersons,
            gson = gson,
            notificationService = notificationService,
            repoRelations = repoRelations,
            cryptoService = cryptoService
        )
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()


    @Provides
    @Singleton
    fun provideCryptoService(repo: RepoCipher): CryptoService = CryptoCipher(repo)

    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext appContext: Context,
        repoPersons: RepoPersons,
    ): NotificationService =
        AndroidNotificationService(appContext, repoPersons)

    //region REPOS

    @Provides
    @Singleton
    fun provideRepoMsgs(dao: Dao): RepoMessages = RepoMessagesImpl(dao)

    @Provides
    @Singleton
    fun provideRepoUtils(@ApplicationContext appContext: Context, gson: Gson): RepoUtils =
        RepoUtilImpl(appContext, gson)

    @Provides
    @Singleton
    fun provideRepoAssets(@ApplicationContext appContext: Context): RepoAssets =
        WebAssetsRepo(appContext)

    @Provides
    @Singleton
    fun provideRepoStorage(@ApplicationContext appContext: Context, dao: Dao): RepoStorage =
        RepoFileStorage(appContext, dao)

    @Provides
    @Singleton
    fun provideRepoRelation(dao: Dao): RepoRelations = RepoRelationImpl(dao)

    @Provides
    @Singleton
    fun provideRepoPersons(dao: Dao): RepoPersons = RepoPersonRoom(dao)

    @Provides
    @Singleton
    fun provideRepoCrypto(dao: Dao): RepoCipher = RoomCipherRepo(dao)

    @Provides
    @Singleton
    fun provideRepoMedia(dao: Dao): RepoMedia = RepoSavedMedia(dao)

    //endregion REPOS

    //region ROOM


    @Provides
    @Singleton
    fun provideMessageDao(appDatabase: Database): Dao = appDatabase.messageDao()


    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): Database {
        return Room.databaseBuilder(
            appContext,
            Database::class.java,
            "MSG_DB"
        )
            .allowMainThreadQueries()
            .build()
    }

    //endregion ROOM
}