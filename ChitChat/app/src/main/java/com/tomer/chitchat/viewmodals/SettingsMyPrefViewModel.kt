package com.tomer.chitchat.viewmodals

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.R
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsMyPrefViewModel @Inject constructor(
    private val repoUtils: RepoUtils,
    private val retro: Api,
    private val repoStorage: RepoStorage,
) : ViewModel() {

    val pref = repoUtils.getPrefs()
    val phone: String = Utils.myPhone

    //region CURRENT PREF

    private val _myPrefs = MutableLiveData<MyPrefs>()
    val myPrefs: LiveData<MyPrefs> = _myPrefs
    fun setName(name: String) {
        viewModelScope.launch {
            if (name.isEmpty()) {
                flowEvents.emit(SettingEvents.ERROR_NAME to "")
                return@launch
            }
            val isUpdated = retro.updateName(name).body().toString() == "Updated"
            if (isUpdated) {
                _myPrefs.value?.name = name
                repoUtils.savePrefs(repoUtils.getPrefs().also { it.name = name })
                flowEvents.emit(Pair(SettingEvents.SHOW_TOAST, "Name updated..."))
            } else {
                flowEvents.emit(Pair(SettingEvents.UPDATE_PREF, ""))
                flowEvents.emit(Pair(SettingEvents.SHOW_TOAST, "Connection error"))
            }

        }
    }

    fun setAbout(a: String) {
        viewModelScope.launch {
            if (a.isEmpty()) {
                flowEvents.emit(SettingEvents.ERROR_ABOUT to "")
                return@launch
            }
            val about = ConversionUtils.encode(a)
            val isUpdated = retro.updateAbout(about).body().toString() == "Updated"
            if (isUpdated) {
                _myPrefs.value?.about = about
                repoUtils.savePrefs(repoUtils.getPrefs().also { it.about = about })
                flowEvents.emit(Pair(SettingEvents.SHOW_TOAST, "About updated..."))
            } else {
                flowEvents.emit(Pair(SettingEvents.UPDATE_PREF, ""))
                flowEvents.emit(Pair(SettingEvents.SHOW_TOAST, "Connection error"))
            }

        }
    }

    //endregion CURRENT PREF

    // region QR VIEW

    private val _qrDia = MutableLiveData<Boolean>()
    val qrDia: LiveData<Boolean> = _qrDia
    fun toggleQr() = _qrDia.postValue(qrDia.value?.not() ?: true)

    val qrAss = listOf(
        R.drawable.pattern_1, R.drawable.pattern_2, R.drawable.pattern_3, R.drawable.pattern_4, R.drawable.pattern_5,
        R.drawable.pattern_6, R.drawable.pattern_7, R.drawable.pattern_8, R.drawable.pattern_9, R.drawable.pattern_10
    )

    //endregion QR VIEW

    //region PROGRESS

    private val _textSize = MutableLiveData<Float>()
    val textSize: LiveData<Float> = _textSize
    fun setProgTextSize(value: Float) =
        _textSize.postValue(value).also { repoUtils.savePrefs(repoUtils.getPrefs().also { it.textSize = value }) }

    private val _corners = MutableLiveData<Float>()
    val corners: LiveData<Float> = _corners
    fun setProgCorners(value: Float) =
        _corners.postValue(value).also { repoUtils.savePrefs(repoUtils.getPrefs().also { it.msgItemCorners = value }) }

    private val _parallax = MutableLiveData(pref.parallaxFactor)
    val parallax: LiveData<Float> = _parallax
    fun setParallax(value: Float) =
        _parallax.postValue(value).also { repoUtils.savePrefs(repoUtils.getPrefs().also { it.parallaxFactor = value }) }


    //endregion PROGRESS

    //region DP

    private val _dpFile = MutableLiveData<File>(null)
    val dpFile: LiveData<File> = _dpFile

    private val _dpUploadProg = MutableLiveData(false)
    val dpUploadProg: LiveData<Boolean> = _dpUploadProg

    fun uploadDp(dp: Bitmap) {
        _dpUploadProg.postValue(true)
        viewModelScope.launch {
            val bytes = ConversionUtils.convertToWebp(dp)
            val reqBody = bytes
                .toRequestBody(
                    "multipart/form-data".toMediaTypeOrNull(),
                    0, bytes.size
                )
            try {
                val res = retro.uploadProfileImage(
                    MultipartBody.Part.createFormData(
                        "file", Utils.myPhone, reqBody
                    )
                ).body().toString()
                if (res.startsWith("Uploaded")) {
                    repoStorage.deleteDP(phone)
                    _dpFile.postValue(
                        repoStorage.saveDP(
                            phone,
                            try {
                                res.split('-')[1].toInt()
                            } catch (_: Exception) {
                                1
                            }, bytes
                        ).also {
                            _dpUploadProg.postValue(false)
                            flowEvents.emit(Pair(SettingEvents.SHOW_TOAST, "Profile updated successfully"))
                            flowEvents.emit(Pair(SettingEvents.DP_UPLOADED, it.absolutePath))
                        }
                    )
                }
            } catch (_: Exception) {
                viewModelScope.launch {
                    _dpUploadProg.postValue(false)
                    flowEvents.emit(Pair(SettingEvents.SHOW_TOAST, "Failed to upload Network error..."))
                    _dpFile.postValue(repoStorage.getDP(phone, sync = true))
                }
            }
        }
    }

    //endregion DP

    val flowEvents = MutableSharedFlow<Pair<SettingEvents, String>>()

    init {
        _myPrefs.postValue(pref)
        _corners.postValue(pref.msgItemCorners)
        _textSize.postValue(pref.textSize)
        viewModelScope.launch {
            _dpFile.postValue(repoStorage.getDP(phone, sync = true))
        }
    }


    enum class SettingEvents {
        UPDATE_PREF, SHOW_TOAST, ERROR_NAME, ERROR_ABOUT, DP_UPLOADED
    }
}