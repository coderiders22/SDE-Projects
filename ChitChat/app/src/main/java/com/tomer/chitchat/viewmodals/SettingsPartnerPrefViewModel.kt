package com.tomer.chitchat.viewmodals

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.modals.prefs.PartnerPrefBuilder
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.qrProvider.GradModel
import com.tomer.chitchat.utils.qrProvider.RenderModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.core.graphics.toColorInt

@HiltViewModel
class SettingsPartnerPrefViewModel @Inject constructor(
    private val repoMsgs: RepoMessages,
    private val repoStorage: RepoStorage,
    private val repoPersons: RepoPersons,
    repoUtils: RepoUtils,
) : ViewModel() {

    var isChanged = false

    //region STATES

    private val _partnerPref = MutableLiveData<ModelPartnerPref>()
    val partnerPref: LiveData<ModelPartnerPref> = _partnerPref

    private val _transparency = MutableLiveData(1f)
    val transparency: LiveData<Float> = _transparency

    private val _sharedContent = MutableLiveData<List<Triple<MsgMediaType, File, Float?>>>(listOf())
    val sharedContent: LiveData<List<Triple<MsgMediaType, File, Float?>>> = _sharedContent

    //endregion STATES

    //region THEME DATA

    val rvDoodle = listOf(
        1 to RenderModel(1f, Color.GREEN, GradModel(120, "#a6c0fe".toColorInt(), "#f68084".toColorInt())),
        2 to RenderModel(1f, Color.GREEN, GradModel(45, "#cf5e0c".toColorInt(), "#ee0979".toColorInt())),
        3 to RenderModel(1f, Color.GREEN, GradModel(90, "#642b73".toColorInt(), "#c6426e".toColorInt())),
        4 to RenderModel(1f, Color.GREEN, GradModel(45, "#cb356b".toColorInt(), "#bd3f32".toColorInt())),
        5 to RenderModel(1f, Color.GREEN, GradModel(125, "#283c86".toColorInt(), "#45a247".toColorInt())),
        6 to RenderModel(1f, Color.GREEN, GradModel(100, "#4b639f".toColorInt(), "#5baeb4".toColorInt())),
        7 to RenderModel(1f, Color.GREEN, GradModel(15, "#cf5e0c".toColorInt(), "#ee0979".toColorInt())),
        8 to RenderModel(1f, Color.GREEN, GradModel(45, "#0f3443".toColorInt(), "#34e89e".toColorInt())),
        9 to RenderModel(1f, Color.GREEN, GradModel(125, "#935096".toColorInt(), "#4568dc".toColorInt())),
        10 to RenderModel(1f, Color.GREEN, GradModel(95, "#cf5e0c".toColorInt(), "#ee0979".toColorInt())),
        11 to RenderModel(1f, Color.GREEN, GradModel(25, "#fefdcd".toColorInt(), "#a3e6ff".toColorInt())),
        12 to RenderModel(1f, Color.GREEN, GradModel(220, "#bdc3c7".toColorInt(), "#2c3e50".toColorInt())),
    )

    val rvAccent = listOf(
        RenderModel(1f, "#005feb".toColorInt()),
        RenderModel(1f, "#28a359".toColorInt()),
        RenderModel(1f, Color.GREEN, GradModel(100, "#6573f8".toColorInt(), "#a751a8".toColorInt())),
        RenderModel(1f, "#8366cc".toColorInt()),
        RenderModel(1f, Color.GREEN, GradModel(30, "#2f0743".toColorInt(), "#41295a".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(45, "#358669".toColorInt(), "#4d8f51".toColorInt())),
        RenderModel(1f, "#d28036".toColorInt()),
        RenderModel(1f, Color.GREEN, GradModel(100, "#4b639f".toColorInt(), "#5baeb4".toColorInt())),
    )

    val rvBgRenders = listOf(
        RenderModel(1f, Color.GREEN, GradModel(120, "#a6c0fe".toColorInt(), "#f68084".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(45, "#cf5e0c".toColorInt(), "#ee0979".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(90, "#642b73".toColorInt(), "#c6426e".toColorInt())),
        RenderModel(1f, "#c678dd".toColorInt()),
        RenderModel(1f, Color.GREEN, GradModel(125, "#283c86".toColorInt(), "#45a247".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(15, "#cf5e0c".toColorInt(), "#ee0979".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(45, "#0f3443".toColorInt(), "#34e89e".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(80, "#eacda3".toColorInt(), "#d6ae7b".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(95, "#cf5e0c".toColorInt(), "#ee0979".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(45, "#cb356b".toColorInt(), "#bd3f32".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(125, "#935096".toColorInt(), "#4568dc".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(100, "#4b639f".toColorInt(), "#5baeb4".toColorInt())),
        RenderModel(1f, "#d28036".toColorInt()),
        RenderModel(1f, Color.GREEN, GradModel(25, "#fefdcd".toColorInt(), "#a3e6ff".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(220, "#bdc3c7".toColorInt(), "#2c3e50".toColorInt())),
        RenderModel(1f, Color.GREEN, GradModel(125, "#f7ff00".toColorInt(), "#db36a4".toColorInt())),
    )

    //endregion THEME DATA

    val myPref: MyPrefs = repoUtils.getPrefs()
    var phone = ""

    fun loadPref(phone: String) {
        this.phone = phone
        viewModelScope.launch {
            val pref = repoPersons.getPersonPref(phone)
                ?: PartnerPrefBuilder(phone).build()
            _partnerPref.postValue(pref)
            _transparency.postValue(pref.background.alpha)
        }
        if (_sharedContent.value?.isEmpty() == true)
            viewModelScope.launch {
                val listFiles = mutableListOf<Triple<MsgMediaType, File, Float?>>()
                repoMsgs.getMsgsOfUserOnlyMedia(phone)
                    .forEach {
                        repoStorage.getFileFromFolder(it.msgType, it.mediaFileName ?: "")
                            ?.let { file ->
                                listFiles.add(Triple(it.msgType, file, it.aspectRatio))
                            }
                    }
                _sharedContent.postValue(listFiles)
            }
    }

    fun setDimming(value: Float) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.background.alpha = value
        _transparency.postValue(value)
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
        }
    }

    fun setBackGround(assetNo: Int, renderModel: RenderModel?) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.backgroundAssetNo = assetNo
        if (renderModel != null)
            pref.background = renderModel
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
            _transparency.postValue(1f)
        }
    }

    fun setBackGroundIndex(index: Int) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.background =
            rvBgRenders.getOrNull(index) ?: RenderModel(1f, "#005feb".toColorInt())
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
            _transparency.postValue(1f)
        }
    }

    fun setAccent(index: Int) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.accent = rvAccent.getOrNull(index) ?: RenderModel(1f, "#005feb".toColorInt())
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
        }
    }

    fun setNotification(show: Boolean) {
        viewModelScope.launch {
            _partnerPref.value?.let {
                it.notificationAllowed = show
                repoPersons.insertPersonPref(it)
            }
        }
    }

    fun setChatLock(locked: Boolean) {
        viewModelScope.launch {
            _partnerPref.value?.let {
                it.chatLocked = locked
                repoPersons.insertPersonPref(it)
            }
        }
    }

}
