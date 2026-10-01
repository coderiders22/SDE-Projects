package com.tomer.chitchat.repo.impl

import android.content.Context
import com.google.gson.Gson
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.utils.Utils
import androidx.core.content.edit

class RepoUtilImpl(
    private val context: Context,
    private val gson: Gson
) : RepoUtils {

    private val myPrefMod by lazy { loadMyPref() }
    private val prefUtils by lazy { context.getSharedPreferences("utils", Context.MODE_PRIVATE) }

    override fun getToken(): String =
        prefUtils.getString("token", "").toString()

    override fun saveToken(token: String) {
        prefUtils.edit { putString("token", token) }
    }

    override fun getTime() = prefUtils.getLong("time", 0)

    override fun saveTime(timeMillis: Long) = prefUtils.edit { putLong("time", timeMillis) }

    override fun getPrefs() = myPrefMod

    private fun loadMyPref(): MyPrefs {
        return try {
            gson.fromJson(
                prefUtils.getString("myPrefs", null) ?: throw Exception(),
                MyPrefs::class.java
            )
        } catch (e: Exception) {
            MyPrefs(Utils.myPhone, "", "", 12f, 18f, 1, 3.2f)
        }
    }

    override fun savePrefs(mod: MyPrefs) {
        myPrefMod.copyFrom(mod)
        prefUtils.edit { putString("myPrefs", gson.toJson(myPrefMod)) }
    }

    override fun getPhone() = prefUtils.getString("phone", "").toString()

    override fun savePhone(phone: String) =
        prefUtils.edit { putString("phone", phone) }

    override fun getTempId(): Long {
        val tempId = prefUtils.getLong("tempId", 0L)
        prefUtils.edit { putLong("tempId", tempId + 1) }
        return tempId
    }

}