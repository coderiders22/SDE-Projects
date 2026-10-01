package com.tomer.chitchat.viewmodals

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val retro: Api,
    private val repo: RepoUtils
) : ViewModel() {

    private val _codeSend = MutableLiveData<Boolean>()
    val codeSend: LiveData<Boolean> = _codeSend

    private val _loginProg = MutableLiveData<Boolean>()
    val loginProg: LiveData<Boolean> = _loginProg

    private val _showSelGallery = MutableLiveData<Boolean>()
    val showSelGallery: LiveData<Boolean> = _showSelGallery

    private val _done = MutableLiveData<Boolean>()
    val done: LiveData<Boolean> = _done

    private val _selectedImg = MutableLiveData<Uri>()
    val selectedImg: LiveData<Uri> = _selectedImg

    private val _resendOtpButton = MutableLiveData(false)
    val resendOtpButton: LiveData<Boolean> = _resendOtpButton

    private val _storagePermission = MutableLiveData(false)
    val storagePermission: LiveData<Boolean> = _storagePermission

    private val _resendOtpTimer = MutableLiveData(40)
    val resendOtpTimer: LiveData<Int> = _resendOtpTimer

    private val _currentFrag = MutableLiveData(1)
    val currentFrag: LiveData<Int> = _currentFrag

    //region FRAG_OTP
    private val _phone = MutableLiveData<String>()
    val phone: LiveData<String> = _phone

    private val _showProgSendOtpFrag = MutableLiveData(false)
    val showProg: LiveData<Boolean> = _showProgSendOtpFrag
    //endregion FRAG_OTP

    val flowToasts = MutableSharedFlow<String>()

    var name = ""
    var otpTemp = ""
    private var jobTimer: Job = viewModelScope.launch { }

    fun loginWithOtp(otp: String) {
        _loginProg.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val firebaseNotificationToken = async { getFirebaseNotificationToken() }
                try {
                    val token = retro.getLoginToken(otp, firebaseNotificationToken.await())
                    if (token.code() == 403) {
                        flowToasts.emit("Invalid OTP")
                        _loginProg.postValue(false)
                        return@withContext
                    }
                    if (token.isSuccessful) {
                        token.body()?.let {
                            repo.saveToken(it.token)
                            repo.getPrefs().apply {
                                this.phone = _phone.value.toString().trim()
                                this.about = it.about
                                this.name = it.name
                                repo.savePrefs(this)
                            }
                            name = it.name
                        }
                        _currentFrag.postValue(3)
                        jobTimer.cancel()
                        _loginProg.postValue(false)
                    } else {
                        _loginProg.postValue(false)
                    }
                } catch (e: Exception) {
                    _loginProg.postValue(false)
                    e.printStackTrace()
                }
            }
        }
    }

    fun showGallery(bool: Boolean) = _showSelGallery.postValue(bool)
    fun imgPicked(uri: Uri) = _selectedImg.postValue(uri)

    private suspend fun uploadImage(bytes: ByteArray) {
        val reqBody = bytes
            .toRequestBody(
                "multipart/form-data".toMediaTypeOrNull(),
                0, bytes.size
            )
        try {
            retro.uploadProfileImage(
                MultipartBody.Part.createFormData(
                    "file", Utils.myPhone, reqBody
                )
            )
        } catch (_: Exception) {
        }
    }

    fun setStoragePermission(isGiven: Boolean) {
        _storagePermission.postValue(isGiven)
    }

    fun setCurrentFrag(no: Int) {
        _currentFrag.postValue(no)
    }

    fun setPhone(phone: String) {
        _phone.value = phone
    }

    init {
        setPhone(repo.getPhone())
    }

    private var currentRemTime = 40
    private fun createNewCountDownJob(): Job {
        currentRemTime = 40
        _resendOtpButton.postValue(false)
        return viewModelScope.launch {
            while (currentRemTime != 0) {
                delay(1000)
                _resendOtpTimer.postValue(--currentRemTime)
            }
            _resendOtpButton.postValue(true)
            _resendOtpTimer.postValue(40)
        }
    }

    fun login(name: String, bmp: Bitmap?) {
        _loginProg.postValue(true)
        Utils.myName = name
        viewModelScope.launch {
            val res1 = async {
                retro.updateName(name).body().toString()
            }
            if (bmp == null) {
                res1.await()
                repo.savePrefs(MyPrefs(phone.value.toString(), name, "", 12f, 18f, 0, 4f))
                _loginProg.value = false
                _done.postValue(true)
                return@launch
            }
            val res2 = async {
                uploadImage(ConversionUtils.convertToWebp(bmp))
            }
            res2.await()
            res1.await()
            repo.savePrefs(MyPrefs(phone.value.toString(), name, "", 12f, 18f, 0, 4f))
            _loginProg.postValue(false)
            _done.postValue(true)
        }
    }

    fun sendOtp() {
        viewModelScope.launch {
            _showProgSendOtpFrag.value = true
            val sendOtpRes = retro.sendOtp(_phone.value.toString().trim())
            if (sendOtpRes.isSuccessful) {
                _codeSend.value = true
                _loginProg.value = false
                jobTimer.cancel()
                jobTimer = createNewCountDownJob()
                setCurrentFrag(2)
            } else {
                Log.d("TAG--", "sendOtp: ${sendOtpRes.raw()}")
                _showProgSendOtpFrag.value = false
                _loginProg.value = false
                _codeSend.value = false
                flowToasts.emit("Something went wrong...")
            }
        }
    }

    private suspend fun getFirebaseNotificationToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        continuation.resumeWith(Result.success(task.result))
                    else continuation.resumeWith(Result.failure(task.exception!!))
                }
        }
    }
}