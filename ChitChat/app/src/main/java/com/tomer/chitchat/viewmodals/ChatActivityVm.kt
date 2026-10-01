package com.tomer.chitchat.viewmodals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.modals.prefs.PartnerPrefBuilder
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatActivityVm @Inject constructor(
    private val repoPersons: RepoPersons,
    private val repoStorage: RepoStorage,
    private val repoMsg: RepoMessages,
    repoUtils: RepoUtils,
) : ViewModel() {

    //region SELECTION HANDLING

    val flowDeleteIds = MutableSharedFlow<Long>()

    private val _headMenu = MutableLiveData<Boolean>()
    val headMenu: LiveData<Boolean> = _headMenu

    private val _selCount = MutableLiveData(0)
    val selCount: LiveData<Int> = _selCount

    val selectedMsgIds = mutableListOf<Long>()

    fun addDelNo(msgId: Long): Boolean {
        val i = selectedMsgIds.indexOfFirst { msgId == it }
        return if (i == -1) {
            selectedMsgIds.add(msgId)
            true
        } else {
            selectedMsgIds.removeIf { msgId == it }
            false
        }.also {
            _selCount.postValue(selectedMsgIds.size)
            if (selectedMsgIds.isEmpty()) {
                if (headMenu.value != false)
                    _headMenu.postValue(false)
            } else
                if (headMenu.value != true)
                    _headMenu.postValue(true)
        }
    }

    fun delSelected(isDel: Boolean) {
        if (isDel) {
            viewModelScope.launch {
                val mediaFiles = mutableListOf<Pair<String, MsgMediaType>>()
                val lastMsg = repoMsg.getLastMsgForPartner(phone)
                var isNeedToSetLastMsg = false
                selectedMsgIds.forEach { id ->
                    if (id == lastMsg?.id) isNeedToSetLastMsg = true
                    flowDeleteIds.emit(id)
                    val msg = repoMsg.getMsg(id)
                    repoMsg.deleteById(id)
                    msg?.mediaFileName?.let { mediaFiles.add(Pair(it, msg.msgType)) }
                }
                withContext(Dispatchers.IO) {
                    if (isNeedToSetLastMsg) {
                        repoPersons.getPersonByPhone(phone)?.let {
                            ModelRoomPersons(
                                phone, it.name,
                                MsgMediaType.TEXT, "", -1L,
                                System.currentTimeMillis(),
                                lastSeenMillis = it.lastSeenMillis,
                                isSent = false,
                                msgStatus = MsgStatus.RECEIVED
                            ).apply { repoPersons.insertPerson(this) }
                        }
                    }
                    mediaFiles.forEach { pair ->
                        repoStorage.deleteFile(pair.first, pair.second)
                    }
                }
            }
        }

        selectedMsgIds.clear()
        _selCount.postValue(0)
        _headMenu.postValue(false)

    }

    fun delMsg(msgId: Long) {
        viewModelScope.launch {
            val mediaFiles = mutableListOf<Pair<String, MsgMediaType>>()
            val lastMsg = repoMsg.getLastMsgForPartner(phone)
            var isNeedToSetLastMsg = false
            val msgR = repoMsg.getMsg(msgId) ?: return@launch

            msgR.id.also { id ->
                if (id == lastMsg?.id) isNeedToSetLastMsg = true
                flowDeleteIds.emit(id)
                repoMsg.deleteById(id)
                msgR.mediaFileName?.let { mediaFiles.add(Pair(it, msgR.msgType)) }
            }
            withContext(Dispatchers.IO) {
                if (isNeedToSetLastMsg) {
                    repoPersons.getPersonByPhone(phone)?.let {
                        ModelRoomPersons(
                            phone, it.name,
                            MsgMediaType.TEXT, "", -1L,
                            System.currentTimeMillis(),
                            lastSeenMillis = it.lastSeenMillis,
                            isSent = false,
                            msgStatus = MsgStatus.RECEIVED
                        ).apply { repoPersons.insertPerson(this) }
                    }
                }
                mediaFiles.forEach { pair ->
                    repoStorage.deleteFile(pair.first, pair.second)
                }
            }
        }
    }

    //endregion SELECTION HANDLING

    //region STATES

    private val _navBottom = MutableLiveData(false)
    val navBottom: LiveData<Boolean> = _navBottom

    fun setNavBottom(value: Boolean) {
        _navBottom.postValue(value)
    }

    val scrollPosition = MutableLiveData(0)

    private val _replyMsgData = MutableLiveData<UiMsgModal?>(null)
    val replyMsgData: LiveData<UiMsgModal?> = _replyMsgData

    fun setReplyData(data: UiMsgModal) {
        _replyMsgData.postValue(data)
    }

    fun removeReplyData() = _replyMsgData.postValue(null)

    var replyClickID = -1L

    private val _dpFile = MutableLiveData<File>()
    val dpFile: LiveData<File> = _dpFile

    private val _lastSeenText = MutableLiveData("")
    val lastSeenText: LiveData<String> = _lastSeenText

    fun setTypingText(text: String) {
        _lastSeenText.postValue(text)
    }
    //endregion STATES

    var phone = ""
    val myPref: MyPrefs = repoUtils.getPrefs()

    private val _partnerPref = MutableLiveData<ModelPartnerPref>()
    val partnerPref: LiveData<ModelPartnerPref> = _partnerPref

    fun reloadPartnerPref() {
        viewModelScope.launch {
            _partnerPref.postValue(repoPersons.getPersonPref(phone) ?: PartnerPrefBuilder(phone).build())
        }
    }

    fun setPartnerNo(phone: String) {
        this.phone = phone
        viewModelScope.launch {
            _dpFile.postValue(repoStorage.getDP(phone))
        }
        viewModelScope.launch {
            _partnerPref.postValue(repoPersons.getPersonPref(phone) ?: PartnerPrefBuilder(phone).build())
        }
    }
}