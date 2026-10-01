package com.tomer.chitchat.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowInsets
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.AdapPerson
import com.tomer.chitchat.adap.AdapPerson.Companion.getDrawableId
import com.tomer.chitchat.databinding.ActivityMainBinding
import com.tomer.chitchat.databinding.BarcodeDiaBinding
import com.tomer.chitchat.databinding.RowPersonBinding
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.hideKeyBoard
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import com.tomer.chitchat.utils.Utils.Companion.isLandscapeOrientation
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.utils.acceptNo
import com.tomer.chitchat.viewmodals.AssetsViewModel
import com.tomer.chitchat.viewmodals.MainViewModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AdapPerson.CallbackClick, View.OnClickListener {

    private val REQ_CODE_PROILE_CHANGE = 1020
    private val b by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModal: MainViewModal by viewModels()
    private val assetsVM: AssetsViewModel by viewModels()

    private val adapter by lazy { AdapPerson(this, this) }
    private lateinit var ll: LinearLayoutManager

    private val qrDia by lazy { crQr() }
    private lateinit var barcodeView: CompoundBarcodeView
    private val callback by lazy { callBack() }

    private var activityLife = false


    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModal.isNameSet()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(b.root)
        enableEdgeToEdge()
        setupKeyboardAnimation()
        if (isLandscapeOrientation()) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            else {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                actionBar?.hide()
            }
        }
        Utils.myPhone = viewModal.getPhone()
        if (isDarkModeEnabled()) b.tvAppName.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.white
            )
        )
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null)
                b.root.post { connectFromQrData(uri.toString()) }
        }
        b.apply {
            btConnect.setOnClickListener(this@MainActivity)
            imgBarcode.setOnClickListener(this@MainActivity)
            imgFab.setOnClickListener(this@MainActivity)
            btCross.setOnClickListener(this@MainActivity)

            btDel.setOnClickListener(this@MainActivity)
            btBack.setOnClickListener(this@MainActivity)

            btSearch.setOnClickListener(this@MainActivity)
            btProfile.setOnClickListener(this@MainActivity)
        }

        ll = LinearLayoutManager(this@MainActivity)
        b.rvPersons.layoutManager = ll
        b.rvPersons.adapter = adapter

        viewModal.persons.observe(this) {
            adapter.submitList(it)
        }
        lifecycleScope.launch {
            assetsVM.flowEvents.collectLatest {
                runOnUiThread {
                    when (it.type) {
                        FlowType.SHOW_BIG_GIF -> if (activityLife) {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            Glide.with(this@MainActivity)
                                .load(it.fileGif!!)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(b.imgLottie)
                        }

                        FlowType.SHOW_BIG_JSON -> if (activityLife) {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            try {
                                b.imgLottie.setAnimationFromJson(
                                    it.data!!.msg,
                                    it.data.mediaFileName
                                )
                                b.imgLottie.playAnimation()
                            } catch (_: Exception) {
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModal.flowMsgs.collectLatest {
                runOnUiThread {
                    handelFlowMsg(it)
                }
            }
        }

        viewModal.headMenu.observe(this@MainActivity) {
            b.apply {
                btProfile.isClickable = false
                btSearch.isClickable = false
                btBack.isClickable = false
                btDel.isClickable = false
            }
            if (it) {
                val width = b.layMainHead.width
                val height = b.layMainHead.height
                b.laySelHead.visibility = View.VISIBLE
                b.apply {
                    btBack.isClickable = true
                    btDel.isClickable = true
                }
                if (b.laySelHead.isAttachedToWindow)
                    ViewAnimationUtils.createCircularReveal(
                        b.laySelHead,
                        width.times(0.8f).toInt(),
                        height.shr(1),
                        1f,
                        width.toFloat()
                    ).apply {
                        duration = 340
                        start()
                    }
            } else {
                val width = b.laySelHead.width
                val height = b.laySelHead.height
                if (b.laySelHead.isAttachedToWindow)
                    ViewAnimationUtils.createCircularReveal(
                        b.laySelHead,
                        width.times(0.8f).toInt(),
                        height.shr(1),
                        width.toFloat(),
                        1f
                    ).apply {
                        duration = 200
                        doOnEnd {
                            b.apply {
                                laySelHead.visibility = View.GONE
                                btProfile.isClickable = true
                                btSearch.isClickable = true
                            }
                        }
                        start()
                    }
                else {
                    b.apply {
                        laySelHead.visibility = View.GONE
                        btProfile.isClickable = true
                        btSearch.isClickable = true
                    }
                }
            }

        }
        viewModal.selCount.observe(this@MainActivity) {
            b.root.post {
                b.tvSelCount.text = it.toString()
            }
        }

        viewModal.fabView.observe(this@MainActivity) {
            if (it) {
                b.layNewNumber.visibility = View.VISIBLE
                b.imgBarcode.playAnimation()
                b.imgFab.visibility = View.GONE
                b.etNewNumber.requestFocus()
                return@observe
            }

            b.layNewNumber.visibility = View.GONE
            b.imgBarcode.pauseAnimation()
            b.imgFab.visibility = View.VISIBLE
            b.root.postDelayed({
                this.hideKeyBoard()
                b.etNewNumber.clearFocus()
            }, 100)
        }

        val notiMan by lazy { NotificationManagerCompat.from(this) }
        notiMan.cancelAll()

        viewModal.loadMyDp()

        viewModal.openChatPhoneNo.observe(this) {
            if (it.second.isEmpty()) return@observe
            if (it.first) {
                // Initialize the biometric prompt
                val biometricPrompt = BiometricPrompt(
                    this,
                    ContextCompat.getMainExecutor(this),
                    object : BiometricPrompt.AuthenticationCallback() {
                        @OptIn(UnstableApi::class)
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            startActivity(
                                Intent(this@MainActivity, ChatActivity::class.java)
                                    .apply {
                                        putExtra("phone", it.second)
                                    }
                            )
                            viewModal.openChat("")
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            // Handle errors (e.g., show a message)
                            viewModal.openChat("")
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // Handle failed authentication
                            viewModal.openChat("")
                        }
                    })

                // Configure the biometric prompt
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Chat Locked🔒")
                    .setSubtitle("Use fingerprint or face lock to unlock...")
                    .setNegativeButtonText("Cancel")
                    .build()

                // Show the biometric prompt
                biometricPrompt.authenticate(promptInfo)
            } else {
                startActivity(
                    Intent(this@MainActivity, ChatActivity::class.java)
                        .apply {
                            putExtra("phone", it.second)
                        }
                )
                viewModal.openChat("")
            }

        }
    }

    private fun getRvViewIfVisible(phone: String): RowPersonBinding? {
        val list = adapter.currentList
        for (i in list.indices) {
            if (list[i].phoneNo == phone) {
                val view = ll.findViewByPosition(i) ?: return null
                return RowPersonBinding.bind(view)
            }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        activityLife = true
        viewModal.loadPersons(adapter.currentList)
        Utils.currentPartner = null
        b.root.post {
            val insets = ViewCompat.getRootWindowInsets(b.root) ?: return@post
            setPaddingsByInsets(insets)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModal.closeWebSocket()
    }

    override fun onPause() {
        super.onPause()
        activityLife = false
    }

    override fun onBackPressed() {
        if (viewModal.headMenu.value == true) {
            viewModal.delSelected(false, adapter.currentList)
            for (i in adapter.currentList) {
                if (i.isSelected) i.isSelected = false
                val b = getRvViewIfVisible(i.phoneNo) ?: continue
                b.root.setBackgroundColor(ContextCompat.getColor(this, R.color.trans))
            }
            return
        }
        if (b.extendedDpView.isVisible()) {
            b.extendedDpView.setBitmap("")
            return
        }
        super.onBackPressed()
        b.root.postDelayed({ finishAffinity() }, 400)
    }

    override fun onClick(v: View) {
        when (v.id) {
            b.imgBarcode.id -> {
                b.etNewNumber.clearFocus()
                hideKeyBoard()
                if (checkPermission()) {
                    qrDia.show()
                    barcodeView.resume()
                    barcodeView.decodeSingle(callback)
                } else {
                    Dexter.withContext(this)
                        .withPermission(
                            Manifest.permission.CAMERA
                        )
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                qrDia.show()
                                barcodeView.resume()
                                barcodeView.decodeSingle(callback)
                            }

                            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Please Provide with Camera Permission...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                p0: PermissionRequest?,
                                p1: PermissionToken?
                            ) {
                            }

                        }).check()
                }
            }

            b.imgFab.id -> {
                viewModal.setFab(true)
            }

            b.btConnect.id -> {
                val newNo = b.etNewNumber.text.toString().acceptNo()
                if (newNo.length != 10) {
                    b.etNewNumber.error = "Enter Valid Number"
                    return
                }
                lifecycleScope.launch {
                    viewModal.connectNew(
                        newNo,
                        openNextActivity = true,
                        mandatoryConnect = false
                    )
                }
                viewModal.setFab(false)
            }

            b.btCross.id -> {
                viewModal.setFab(false)
            }

            b.btBack.id -> {
                viewModal.delSelected(false, adapter.currentList)
                for (i in adapter.currentList) {
                    if (i.isSelected) i.isSelected = false
                    val b = getRvViewIfVisible(i.phoneNo) ?: continue
                    b.root.setBackgroundColor(ContextCompat.getColor(this, R.color.trans))
                }
            }

            b.btDel.id -> {
                b.btDel.isClickable = false
                viewModal.delSelected(true, adapter.currentList)
            }

            b.btProfile.id -> {
                val options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, b.btProfile, "avatar")
                startActivityForResult(
                    Intent(this, SettingsActivity::class.java),
                    REQ_CODE_PROILE_CHANGE,
                    options.toBundle()
                )
            }

            b.btSearch.id -> {
                //todo apply search impl
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_PROILE_CHANGE && resultCode == RESULT_OK)
            viewModal.loadMyDp()
    }

    override fun onClick(pos: Int) {
        if (viewModal.headMenu.value == true) {
            onLongClick(pos)
            return
        }
        viewModal.openChat(adapter.currentList[pos].phoneNo)
    }

    override fun onClickDp(pos: Int) {
        if (viewModal.headMenu.value == true) {
            onLongClick(pos)
            return
        }
        val br = getRvViewIfVisible(adapter.currentList[pos].phoneNo) ?: return
        val loc = IntArray(2)
        br.imgProfile.getLocationOnScreen(loc)
        b.extendedDpView.setBitmap(
            adapter.currentList[pos].fileDp?.absolutePath ?: "",
            PointF(loc[0].toFloat(), loc[1].toFloat())
        )
    }

    override fun onLongClick(pos: Int) {
        var isSel: Boolean
        adapter.currentList[pos].isSelected =
            viewModal.addDelNo(adapter.currentList[pos].phoneNo).also { isSel = it }
        val b = getRvViewIfVisible(adapter.currentList[pos].phoneNo) ?: return
        val col = if (isSel) ContextCompat.getColor(this, R.color.primary_light)
        else ContextCompat.getColor(this, R.color.backgroundC)
        b.root.setBackgroundColor(col)
    }


    //region BARCODE CALLBACK

    private var flashState = false

    private val gallery: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) {
            if (it == null)
                return@registerForActivityResult
            contentResolver.openInputStream(it).use { ins ->
                val bmp = BitmapFactory.decodeStream(ins)
                val data = scanImageForQR(bmp)
                if (data.isEmpty())
                    Toast.makeText(this, "No Code Found...", Toast.LENGTH_SHORT).show()
                else {
                    connectFromQrData(data)
                    b.layNewNumber.visibility = View.GONE
                    b.imgBarcode.pauseAnimation()
                    b.imgFab.visibility = View.VISIBLE
                }
            }
        }

    private fun scanImageForQR(bmp: Bitmap): String {
        var ret = ""
        val arr = IntArray(bmp.width * bmp.height)
        bmp.getPixels(arr, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val source: LuminanceSource = RGBLuminanceSource(bmp.width, bmp.height, arr)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        try {
            val result = reader.decode(bitmap)
            ret = result.text
        } catch (ignored: java.lang.Exception) {
        }
        return ret
    }

    private fun crQr(): AlertDialog {
        val fb: BarcodeDiaBinding = BarcodeDiaBinding.inflate(layoutInflater)
        val b = AlertDialog.Builder(this)
        b.setView(fb.root)
        val qrd = b.create()
        qrd.window?.attributes?.windowAnimations = R.style.Dialog
        qrd.window?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    R.color.trans
                )
            )
        )
        fb.btCross.setOnClickListener {
            qrd.cancel()
        }
        fb.btGal.setOnClickListener {
            qrd.cancel()
            gallery.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }
        fb.btFlash.setOnClickListener {
            if (flashState) barcodeView.setTorchOff().also { flashState = false }
            else barcodeView.setTorchOn().also { flashState = true }
        }
        fb.barcodeView.setStatusText("")
        barcodeView = fb.barcodeView
        fb.btCross.translationY = 120f
        fb.btGal.translationY = 120f
        fb.btFlash.translationY = 120f
        fb.btCross.animate().translationY(0f).setDuration(400).setStartDelay(400).start()
        fb.btGal.animate().translationY(0f).setDuration(400).setStartDelay(600).start()
        fb.btFlash.animate().translationY(0f).setDuration(400).setStartDelay(800).start()

        qrd.setOnCancelListener {
            barcodeView.pause()
            barcodeView.setTorchOff()
            flashState = false
        }

        return qrd
    }


    private fun callBack() = BarcodeCallback { result ->
        qrDia.cancel()
        connectFromQrData(result.text.toString())
        b.layNewNumber.visibility = View.GONE
        b.imgBarcode.pauseAnimation()
        b.imgFab.visibility = View.VISIBLE
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return result == PackageManager.PERMISSION_GRANTED
    }

    //endregion BARCODE CALLBACK

    private fun connectFromQrData(data: String) {
        if (data.length != 17) return
        val phoneNo = data.substring(7)
        if (!phoneNo.isDigitsOnly()) return
        lifecycleScope.launch {
            viewModal.connectNew(phoneNo, openNextActivity = true, mandatoryConnect = false)
        }
    }

    //region Handel FLOW MSGS

    @OptIn(UnstableApi::class)
    private fun handelFlowMsg(msg: MsgsFlowState) {
        when (msg.type) {
            FlowType.MSG -> if (activityLife && msg.data != null) {

                if (!msg.isLast) return
                if (adapter.currentList.indexOfFirst { it.phoneNo == msg.fromUser } == -1) {
                    viewModal.loadPersons(adapter.currentList)
                    return
                }

                val lastMsg: String = when (msg.data.msgType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> msg.data.msg
                    MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> msg.data.mediaFileName
                        ?: msg.data.msgType.name
                }
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.fore))
                b.tvLastMsg.text = lastMsg.also { b.tvLastMsg.tag = it }
                b.tvUnreadMsgCount.text =
                    b.tvUnreadMsgCount.text.toString().toInt().plus(1).toString()
                b.msgStatus.visibility = View.GONE
                b.tvUnreadMsgCount.visibility = View.VISIBLE
                b.tvTime.setTextColor(ContextCompat.getColor(this, R.color.purple))
                b.tvLastMsg.setTextColor(ContextCompat.getColor(this, R.color.purple))

                when (msg.data.msgType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> b.msgType.visibility = View.GONE
                    MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> b.msgType.visibility =
                        View.VISIBLE
                }

                when (msg.data.msgType) {
                    MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                        val byes = assetsVM.getBytesOfFile(
                            msg.data.msgType,
                            msg.data.mediaFileName.toString()
                        )
                        Glide.with(this@MainActivity)
                            .load(byes ?: AdapPerson.getByteArr(msg.data.msg.split(",-,")[1]))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(b.imgLottie)
                    }

                    MsgMediaType.VIDEO -> {
                        Glide.with(this@MainActivity)
                            .asBitmap()
                            .load(AdapPerson.getByteArr(msg.data.msg.split(",-,")[1]))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(
                                object : CustomTarget<Bitmap>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap>?
                                    ) {
                                        val b1 = getRvViewIfVisible(msg.fromUser) ?: return
                                        b1.imgLottie.setImageBitmap(resource)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                    }

                                    override fun onLoadFailed(errorDrawable: Drawable?) {
                                        val b1 = getRvViewIfVisible(msg.fromUser) ?: return
                                        b1.imgLottie.setImageDrawable(
                                            ContextCompat.getDrawable(
                                                this@MainActivity,
                                                R.drawable.ic_video
                                            )
                                        )
                                    }
                                }
                            )
                    }

                    MsgMediaType.EMOJI -> {
                        val nameGoogleJson =
                            EmojisHashingUtils.googleJHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameGoogleJson.isNullOrEmpty()) {
                            assetsVM.showGoogleJsonViaFlow(nameGoogleJson, msg.fromUser)
                            return
                        }

                        val nameJson =
                            EmojisHashingUtils.jHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameJson.isNullOrEmpty()) {
                            assetsVM.showJsonViaFlow(nameJson, msg.fromUser)
                            return
                        }

                        val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameGif.isNullOrEmpty()) {
                            assetsVM.showGifViaFlow(nameGif, msg.fromUser)
                            return
                        }

                        val nameTeleGif =
                            EmojisHashingUtils.teleHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameTeleGif.isNullOrEmpty()) {
                            assetsVM.showTeleGifViaFlow(nameTeleGif, msg.fromUser)
                            return
                        }
                    }

                    else -> {
                        if (msg.data.msgType == MsgMediaType.FILE) {
                            b.msgType.visibility = View.VISIBLE
                            b.msgType.setImageResource(getDrawableId(msg.data.mediaFileName ?: ""))
                            b.imgLottie.setImageResource(
                                getDrawableId(
                                    msg.data.mediaFileName ?: ""
                                )
                            )
                        } else {
                            b.msgType.visibility = View.GONE
                            b.imgLottie.setImageDrawable(null)
                        }
                    }
                }

            }

            FlowType.SERVER_REC -> {
                val old =
                    adapter.currentList.find { t -> t.lastMsgId == (msg.oldId ?: -2L) } ?: return
                old.lastMsgId = msg.msgId ?: old.lastMsgId
                old.msgStatus = MsgStatus.SENT_TO_SERVER
                handleMsgStatusAnimation(true, msg.fromUser)
            }

            FlowType.PARTNER_REC -> {
                val old =
                    adapter.currentList.find { t -> t.lastMsgId == (msg.msgId ?: -2L) } ?: return
                old.msgStatus = MsgStatus.RECEIVED
                handleMsgStatusAnimation(false, msg.fromUser)
            }

            FlowType.TYPING -> {
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
                "Typing...".also { b.tvLastMsg.text = it }
            }

            FlowType.NO_TYPING -> {
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                if (b.tvLastMsg.text == "Typing...") {
                    if (b.tvUnreadMsgCount.visibility != View.VISIBLE)
                        b.tvLastMsg.setTextColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                R.color.hintCol
                            )
                        )
                    b.tvLastMsg.text = b.tvLastMsg.tag.toString()
                }
            }

            FlowType.SEND_NEW_CONNECTION_REQUEST -> {
                lifecycleScope.launch {
                    viewModal.connectNew(msg.fromUser, false)
                    if (activityLife) b.root.postDelayed(
                        { viewModal.loadPersons(adapter.currentList) },
                        100
                    )
                }
            }


            FlowType.INCOMING_NEW_CONNECTION_REQUEST, FlowType.REQ_ACCEPTED, FlowType.REQ_REJECTED -> if (activityLife) b.root.postDelayed(
                { viewModal.loadPersons(adapter.currentList) },
                100
            )

            FlowType.ONLINE,
            FlowType.OFFLINE -> if (activityLife) {
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                b.onlineIndi.setStatusAnimating(msg.type == FlowType.ONLINE)
            }


            FlowType.OPEN_NEW_CONNECTION_ACTIVITY -> if (activityLife) startActivity(
                Intent(this@MainActivity, ChatActivity::class.java)
                    .apply {
                        putExtra("phone", msg.fromUser)
                    }
            )

            FlowType.SET_DP -> {
                if (msg.fromUser == Utils.myPhone) { //for my profile
                    Glide.with(this)
                        .asBitmap()
                        .load(msg.fileGif)
                        .circleCrop()
                        .override(100)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .into(b.btProfile)
                    return
                }
                //for RV Items
                val b1 = getRvViewIfVisible(msg.fromUser) ?: return
                Glide.with(this)
                    .asBitmap()
                    .load(msg.fileGif)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .override(200)
                    .placeholder(R.drawable.def_avatar)
                    .error(R.drawable.def_avatar)
                    .into(b1.imgProfile)
            }

            else -> {}
        }
    }

    //endregion Handel FLOW MSGS

    private fun handleMsgStatusAnimation(serverRec: Boolean, phone: String) {
        val index = adapter.currentList.indexOfFirst { phone == it.phoneNo }
        if (index == -1) return
        val b = getRvViewIfVisible(phone) ?: return
        val animDur = 200L
        lifecycleScope.launch {
            delay(animDur)
            b.msgStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    this@MainActivity,
                    if (serverRec) R.drawable.ic_tick else R.drawable.ic_double_tick
                )
            )
            b.msgStatus.animate().rotationY(0f).setInterpolator(LinearInterpolator())
                .setDuration(animDur).start()
        }
        b.msgStatus.animate().rotationY(180f).setInterpolator(LinearInterpolator())
            .setDuration(animDur).start()
    }

    private fun setupKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(
            b.root, object : WindowInsetsAnimationCompat.Callback(
                DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    setPaddingsByInsets(insets)
                    return insets
                }
            }
        )
        // Initial insets (important on cold start)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.root.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            if (b.topBar.height <= 62.px.toInt()) {
                val p = b.topBar.layoutParams as ConstraintLayout.LayoutParams
                p.height = p.height + systemBars.top
                b.topBar.layoutParams = p
            }
            b.layMainHead.setPadding(0, systemBars.top, 0, 0)
            b.laySelHead.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setPaddingsByInsets(insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        b.fabChatCard.translationY = -imeInsets.bottom.toFloat()

        b.root.setPadding(
            systemBars.left, 0,
            systemBars.right,
            systemBars.bottom
        )
    }
}