package com.tomer.chitchat.ui.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowInsets
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.AdapPerson
import com.tomer.chitchat.adap.ChatViewEvents
import com.tomer.chitchat.adap.ClickEvents
import com.tomer.chitchat.adap.EmojiAdapter
import com.tomer.chitchat.adap.chat.ChatAdapter
import com.tomer.chitchat.databinding.ActivityChatBinding
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.msgs.NoTyping
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.ui.views.CorneredImageView
import com.tomer.chitchat.ui.views.MsgBackground
import com.tomer.chitchat.ui.views.MsgSwipeCon
import com.tomer.chitchat.ui.views.MsgSwipeCon.SwipeCA
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.getDpLink
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import com.tomer.chitchat.utils.Utils.Companion.isLandscapeOrientation
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.utils.Utils.Companion.showKeyBoard
import com.tomer.chitchat.utils.clipText
import com.tomer.chitchat.viewmodals.AssetsViewModel
import com.tomer.chitchat.viewmodals.ChatActivityVm
import com.tomer.chitchat.viewmodals.ChatViewModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList


@UnstableApi
@SuppressLint("CheckResult")
@AndroidEntryPoint
class ChatActivity : AppCompatActivity(), ChatViewEvents, SwipeCA, View.OnClickListener,
    SensorEventListener {

    //region GLOBALS
    private val b by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val vma: ChatActivityVm by viewModels()
    private val vm: ChatViewModal by viewModels()
    private val vmAssets: AssetsViewModel by viewModels()
    private var deleteOlderMsgsJob = lifecycleScope.launch { }

    private val codePrefs = 1012

    private lateinit var adap: ChatAdapter
    private val emojiAdap by lazy {
        EmojiAdapter {
            sendTextMessage(EmojisHashingUtils.emojiList[it], true)
            vma.removeReplyData()
            b.rvEmojiContainer.animate()
                .x(b.rvEmojiContainer.width.toFloat())
                .setDuration(220)
                .start()
        }
    }

    private lateinit var ll: LinearLayoutManager

    private val timeVisibilityQueue = LinkedList<Pair<Long, Long>>()
    private var replyFadeAnimator: ValueAnimator? = null

    private val options by lazy {
        RequestOptions().apply {
            transform(RoundedCorners(12))
            override(60)
        }
    }
    private var lastSeenMillis = -1L
    private var currentLottieEmoji = ""
    //endregion GLOBALS

    //region PARALLAX SENSOR

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val accelerometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    private val lastValues = FloatArray(2)
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val alpha = .28f // Smoothing factor: closer to 1.0 is smoother but more laggy

            val x = alpha * lastValues[0] + (1 - alpha) * event.values[0]
            val y = alpha * lastValues[1] + (1 - alpha) * event.values[1]

            lastValues[0] = x
            lastValues[1] = y
            b.imgBg.onSensorEvent(x, y)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    //endregion PARALLAX SENSOR

    //region MEDIA IO

    private val videoEditorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 4. Handle the data returned from SecondActivity
            val data: Intent? = result.data
            val videoName = data?.getStringExtra("FILE_NAME") ?: return@registerForActivityResult
            val uri = data.getStringExtra("FILE_URI") ?: return@registerForActivityResult
            val aspect = data.getStringExtra("ASPECT") ?: return@registerForActivityResult
            val videoTime = data.getStringExtra("VIDEO_TIME") ?: return@registerForActivityResult
            Toast.makeText(this, "Received: $videoName", Toast.LENGTH_LONG).show()

            val tempId = vm.getTempId()
            val msgB = ModelMsgSocket.Builder()
            msgB.isReply(b.replyLayout.isVisible)
            msgB.setTimeMillis(System.currentTimeMillis())
            msgB.msgType(MsgMediaType.VIDEO)
            if (b.replyLayout.isVisible) {
                msgB.replyId(vma.replyMsgData.value!!.id)
                msgB.replyMsgType(vma.replyMsgData.value!!.msgType)
                msgB.replyData(vma.replyMsgData.value!!.msg)
                msgB.replyMediaFileName(vma.replyMsgData.value!!.mediaFileName.toString())
            }
            msgB.msgData("Uploading")
            val aspectFloat = runCatching<Float?> { aspect.toFloat() }
                .fold({ f -> if ((f ?: 0f) > 0f) f else null }, { null })
            vmAssets.handelNewVideoMsg(
                Utils.currentPartner?.partnerId ?: "0000000000",
                videoName, this, videoTime, uri.toUri(),
                aspectFloat, msgB.build(), tempId,
                if (b.replyLayout.isVisible) vma.replyMsgData.value!!.bytes else null
            ) { msg ->
                runOnUiThread {
                    adap.addItem(msg.data.also { it?.isUploaded = true } ?: return@runOnUiThread)
                    b.rvMsg.smoothScrollToPosition(0)
                }
            }
            vma.removeReplyData()
            vm.updatePersonModel(msgB.build(), tempId)
        }
    }

    private val mediaPicker: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            b.btGallery.isClickable = true
            if (uri != null) {
                val mimeType = contentResolver.getType(uri)
                if (mimeType != null) {
                    Log.d("TAG--", "ONSEL $mimeType: ")
                    when {
                        mimeType.startsWith("image/gif") -> {
                            sendMediaMsg(uri, MsgMediaType.GIF)
                        }

                        mimeType.startsWith("video/") -> {
                            runOnUiThread {
                                videoEditorLauncher.launch(
                                    Intent(
                                        this,
                                        VideoSendPreviewActivity::class.java
                                    ).apply {
                                        putExtra("uri", uri.toString())
                                        putExtra("partnerPhone", vm.partnerPref?.phone)
                                    })
                            }
                        }

                        else -> {
                            sendMediaMsg(uri, MsgMediaType.IMAGE)
                        }
                    }
                } else {
                    // Fallback for when MIME type is null
                    val head = ByteArray(6)
                    this.contentResolver.openInputStream(uri).use { ins ->
                        ins?.read(head)
                    }
                    val headStr = String(head)
                    if (headStr == "GIF87a" || headStr == "GIF89a")
                        sendMediaMsg(uri, MsgMediaType.GIF)
                    else sendMediaMsg(uri, MsgMediaType.IMAGE)
                }
            }
        }


    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            b.btAttachments.isClickable = true
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
            val uri = result.data?.data ?: return@registerForActivityResult
            val size = contentResolver.openInputStream(uri)?.available() ?: -1
            if (size == -1 || size > 10485760) {
                Toast.makeText(this, "File size too large to upload...", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            sendMediaMsg(uri, MsgMediaType.FILE)
        }

    //endregion MEDIA IO

    //region LIFECYCLE ACTIVITY

    override fun onResume() {
        super.onResume()
        vm.isChatActivityVisible = true
        vm.clearUnreadCount()
        b.root.post {
            val insets = ViewCompat.getRootWindowInsets(b.root) ?: return@post
            setPaddingsByInsets(insets)
        }
        if (accelerometer == null) {
            vma.myPref.parallaxFactor = 0f
            return
        }
        try {
            if (vma.myPref.parallaxFactor > 0f)
                sensorManager.registerListener(this, accelerometer!!, SensorManager.SENSOR_DELAY_UI)
        } catch (_: Exception) {
            vma.myPref.parallaxFactor = 0f
        }
    }

    override fun onPause() {
        super.onPause()
        vm.isChatActivityVisible = false
        runCatching { vma.scrollPosition.postValue(ll.findFirstVisibleItemPosition()) }
        if (vma.myPref.parallaxFactor > 0f)
            runCatching { sensorManager.unregisterListener(this) }
    }

    override fun onBackPressed() {
        if (vma.headMenu.value == true) {
            vma.delSelected(false)
            for (i in vm.chatMsgs) {
                if (i.isSelected) i.isSelected = false
                val b = getRvViewIfPossibleForId(i.id) ?: continue
                b.setBackgroundColor(ContextCompat.getColor(this, R.color.trans))
            }
            return
        }
        if (vma.replyMsgData.value != null) {
            vma.removeReplyData()
            return
        }
        if (isTaskRoot)
            startActivity(Intent(this, MainActivity::class.java))
        vm.sendMsg(NoTyping())
        super.onBackPressed()
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.hasExtra("phone")) {
            finish()
            return
        }
        val phone = intent.getStringExtra("phone").toString()
        if (phone == vma.phone) return
        finish()
        startActivity(intent)
    }

    //endregion LIFECYCLE ACTIVITY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra("phone")) {
            finish()
            return
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(b.root)
        if (isLandscapeOrientation()) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            else {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                actionBar?.hide()
            }
        }
        enableEdgeToEdge()
        setupKeyboardAnimation()

        vm.openChat(intent.getStringExtra("phone")!!, vma.selectedMsgIds)
        vma.setPartnerNo(intent.getStringExtra("phone")!!)
        b.root.post {
            vmAssets.getGifNow()
            vmAssets.setTypingJson()
        }
        //QUEUE SERVICE EXECUTOR
        lifecycleScope.launch {
            while (true) {
                delay(200)
                var removeCount = 0
                if (timeVisibilityQueue.isEmpty())
                    continue

                for (i in timeVisibilityQueue.indices) {
                    if (timeVisibilityQueue[i].first < System.currentTimeMillis()) {
                        removeCount++
                        val b = getRvViewIfPossibleForId(timeVisibilityQueue[i].second) ?: continue
//                        b?.contTime?.visibility = View.GONE
                        b.findViewById<View>(R.id.contTime)?.visibility = View.GONE
                    }
                }
                while (removeCount-- != 0)
                    timeVisibilityQueue.removeFirst()
            }
        }

        if (powerSaveOn()) vma.myPref.parallaxFactor = 0f
        b.imgBg.run {
            if (vma.myPref.parallaxFactor > 0f)
                setParallaxFactor(vma.myPref.parallaxFactor)
        }

        b.etMsg.setKeyboardInputCall { info ->
            if (!vm.canSendMsg) return@setKeyboardInputCall
            val pickingMediaType = if (
                info
                    .description
                    .getMimeType(0)
                    .equals("image/gif")
            ) MsgMediaType.GIF else MsgMediaType.IMAGE
            sendMediaMsg(info.contentUri, pickingMediaType)
        }

        adap = ChatAdapter(this, this, vm.chatMsgs)
        b.rvMsg.adapter = adap
        b.rvMsg.itemAnimator = null

        ll = LinearLayoutManager(this)
        val iT = ItemTouchHelper(MsgSwipeCon(this, this, vm.chatMsgs))
        iT.attachToRecyclerView(b.rvMsg)
        b.rvMsg.setLayoutManager(ll)
        adap.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    if (vma.navBottom.value == false)
                        b.rvMsg.smoothScrollToPosition(0)
                }
            }
        )
        ll.reverseLayout = true

        b.rvMsg.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy < 0 && vma.navBottom.value == false) {
                        if (ll.findFirstVisibleItemPosition() > 0) vma.setNavBottom(true)
                    } else if (dy > 0 && vma.navBottom.value == true) {
                        if (ll.findFirstVisibleItemPosition() == 0) vma.setNavBottom(false)
                    }

                    val totalItemCount = ll.itemCount
                    val lastVisibleItemPosition = ll.findLastVisibleItemPosition()

                    if (vm.chatMsgs.size - lastVisibleItemPosition < 8) {
                        deleteOlderMsgsJob.cancel()
                        if (totalItemCount > 58)
                            vm.loadMoreData(80, vma.selectedMsgIds)
                        else vm.loadMoreData(30, vma.selectedMsgIds)
                    }
                }
            }
        )
        b.rvMsg.post {
            b.rvMsg.scrollToPosition(vma.scrollPosition.value ?: 0)
        }

        b.etMsg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                vm.textChanged()
                b.btAnimHelper.visibility = if (s.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        b.apply {
            btImg.setOnClickListener(this@ChatActivity)
            btSend.setOnClickListener(this@ChatActivity)
            btCloseReplyLay.setOnClickListener(this@ChatActivity)
            btGallery.setOnClickListener(this@ChatActivity)
            btAttachments.setOnClickListener(this@ChatActivity)
            btBack.setOnClickListener(this@ChatActivity)
            btMenu.setOnClickListener(this@ChatActivity)
            cardFlipper.setOnClickListener(this@ChatActivity)
            layDetail.setOnClickListener(this@ChatActivity)

            btImg.setOnLongClickListener {
                if (!vm.canSendMsg)
                    return@setOnLongClickListener false
                sendTextMessage(ConversionUtils.decode(currentLottieEmoji), true)
                true
            }
        }

        //region DELETE MSGS

        b.btBackSel.setOnClickListener(this)
        b.btDel.setOnClickListener(this)
        b.btCopy.setOnClickListener(this)
        b.btReply.setOnClickListener(this)

        lifecycleScope.launch {
            vma.flowDeleteIds.collectLatest { id ->
                val pos = vm.chatMsgs.indexOfFirst { it.id == id }
                if (pos == -1) return@collectLatest

                vm.chatMsgs.removeAt(pos)
                adap.notifyItemRemoved(pos)
            }
        }
        vma.headMenu.observe(this@ChatActivity) {
            b.apply {
                btBackSel.isClickable = false
                btBack.isClickable = false
                btDel.isClickable = false
            }
            if (it) {
                val width = b.layMainHead.width
                val height = b.layMainHead.height
                b.laySelHead.visibility = View.VISIBLE
//                window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundSelBg)
                b.apply {
                    btBackSel.isClickable = true
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
//                window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundC)
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
                                btBack.isClickable = true
                            }
                        }
                        start()
                    }
                else {
                    b.apply {
                        laySelHead.visibility = View.GONE
                        btBack.isClickable = true
                    }
                }
            }

        }
        vma.selCount.observe(this@ChatActivity) {
            b.root.post {
                b.tvSelCount.text = it.toString()
                b.btReply.visibility = if (it > 1) View.GONE else View.VISIBLE
            }
        }
        //endregion DELETE MSGS

        //region REPLY LAY

        vma.replyMsgData.observe(this) { uiMod ->
            if (uiMod == null) {
                b.replyLayout.visibility = View.GONE
                return@observe
            }
            b.apply {
                etMsg.requestFocus()
                replyLayout.visibility = View.VISIBLE
                showKeyBoard()
                b.root.postDelayed({ etMsg.requestFocus() }, 80)
                when (uiMod.msgType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> {
                        tvRep.text = uiMod.msg
                        imgReplyMedia.visibility = View.GONE
                    }

                    MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO -> {
                        imgReplyMedia.visibility = View.VISIBLE
                        Glide.with(tvPartnerNameCard).load(uiMod.bytes).apply(options)
                            .into(imgReplyMedia)
                        tvRep.text = uiMod.mediaFileName
                    }

                    MsgMediaType.FILE -> {
                        imgReplyMedia.visibility = View.VISIBLE
                        Glide.with(tvPartnerNameCard)
                            .load(AdapPerson.getDrawableId(uiMod.mediaFileName ?: "FILE"))
                            .apply(options).into(imgReplyMedia)
                        tvRep.text = uiMod.mediaFileName
                    }
                }
            }
        }

        //endregion REPLY LAY

        b.btScrollToBottom.setOnClickListener(this)
        vma.navBottom.observe(this) {
            if (it == true) {
                if (b.btScrollToBottom.isAttachedToWindow) {
                    b.btScrollToBottom.animate().apply {
                        scaleX(1f)
                        scaleY(1f)
                        setInterpolator(OvershootInterpolator(1.2f))
                        setDuration(240)
                        start()
                    }
                    b.btScrollToBottom.isClickable = true
                } else b.apply {
                    btScrollToBottom.scaleX = 1f
                    btScrollToBottom.scaleY = 1f
                }
            } else {
                if (b.btScrollToBottom.isAttachedToWindow)
                    animateTo0(b.btScrollToBottom)
                else b.apply {
                    btScrollToBottom.scaleX = 0f
                    btScrollToBottom.scaleY = 0f
                }
                //remove all older msgs only first 30 survive
                deleteOlderMsgsJob = lifecycleScope.launch {
                    delay(2000)
                    // Check if there are more than 30 messages to trim
                    if (vma.navBottom.value == false) {
                        if (vm.chatMsgs.size > 30) {
                            val originalSize = vm.chatMsgs.size
                            val itemsToRemove = originalSize - 30

                            // The items are removed from position 30 to the end of the list
                            val only30 = vm.chatMsgs.take(30)
                            vm.chatMsgs.clear()
                            vm.chatMsgs.addAll(only30)

                            // Notify the adapter that a range of items was removed
                            adap.notifyItemRangeRemoved(30, itemsToRemove)
                        }
                    }
                }
            }
        }

        vma.lastSeenText.observe(this) {
            b.tvDetails.text = it
        }

        lifecycleScope.launch {
            vmAssets.flowEvents.collectLatest {
                handleFlow(it)
            }
        }
        lifecycleScope.launch {
            vm.flowMsgs.collectLatest {
                handleFlow(it)
            }
        }
        b.bigJson.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                b.bigJson.animate().scaleX(0f).scaleY(0f).setDuration(320)
                    .setInterpolator(AccelerateInterpolator()).start()
            }
        })

        vma.dpFile.observe(this) {
            if (b.contRelation.isVisible) {
                Glide.with(this@ChatActivity)
                    .asBitmap()
                    .load(it)
                    .override(200)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.def_avatar)
                    .error(R.drawable.def_avatar)
                    .into(b.imgDpCard)
            }
            Glide.with(this@ChatActivity)
                .asBitmap()
                .load(it)
                .override(80)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.def_avatar)
                .error(R.drawable.def_avatar)
                .into(b.imgDp)
        }

        vma.partnerPref.observe(this) { mod ->
            b.imgBg.setData(
                isDarkModeEnabled(),
                mod.background.alpha,
                mod.backgroundAssetNo,
                mod.background.color,
                mod.background.grad
            )
            for (i in 0 until ll.childCount) {
                val b = ll.getChildAt(i) ?: continue
                val bg = b.findViewById<MsgBackground>(R.id.msgBg) ?: continue
                bg.setColor(mod.accent.color, mod.accent.grad)
            }
            if (mod.accent.grad == null) {
                adap.setValues(vma.myPref.textSize, vma.myPref.msgItemCorners.px, mod.accent.color)
                b.btSendBG.setData(null, 100.px, mod.accent.color)
                b.bgLayReply.setData(null, vma.myPref.msgItemCorners.px, mod.accent.color)
                return@observe
            }
            adap.setValues(vma.myPref.textSize, vma.myPref.msgItemCorners.px, mod.accent.grad!!)
            b.btSendBG.setData(null, 100.px, mod.accent.grad!!)
            b.bgLayReply.setData(null, vma.myPref.msgItemCorners.px, mod.accent.grad!!)
        }
    }

    private fun sendTextMessage(text: String, isOnlyEmoji: Boolean) {
        val msgB = ModelMsgSocket.Builder()
        msgB.isReply(b.replyLayout.isVisible)
        msgB.setTimeMillis(System.currentTimeMillis())
        if (b.replyLayout.isVisible) {
            msgB.replyId(vma.replyMsgData.value!!.id)
            msgB.replyMsgType(vma.replyMsgData.value!!.msgType)
            msgB.replyData(vma.replyMsgData.value!!.msg)
            msgB.replyMediaFileName(vma.replyMsgData.value!!.mediaFileName.toString())
        }
        msgB.msgType(if (isOnlyEmoji) MsgMediaType.EMOJI else MsgMediaType.TEXT)
        msgB.msgData(text)
        b.rvMsg.smoothScrollToPosition(0)
        vm.sendChatMsg(
            msgB.build(),
            if (b.replyLayout.isVisible) vma.replyMsgData.value!!.bytes else null
        )
    }

    private fun sendMediaMsg(uri: Uri, pickingMediaType: MsgMediaType) {
        val tempId = vm.getTempId()
        val msgB = ModelMsgSocket.Builder()
        msgB.isReply(b.replyLayout.isVisible)
        msgB.setTimeMillis(System.currentTimeMillis())
        msgB.msgType(pickingMediaType)
        if (b.replyLayout.isVisible) {
            msgB.replyId(vma.replyMsgData.value!!.id)
            msgB.replyMsgType(vma.replyMsgData.value!!.msgType)
            msgB.replyData(vma.replyMsgData.value!!.msg)
            msgB.replyMediaFileName(vma.replyMsgData.value!!.mediaFileName.toString())
        }
        msgB.msgData("Uploading")
        vmAssets.handleNewMediaMsg(
            Utils.currentPartner?.partnerId ?: "0000000000",
            pickingMediaType, uri, this,
            msgB.build(), tempId,
            if (b.replyLayout.isVisible) vma.replyMsgData.value!!.bytes else null
        ) { msg ->
            runOnUiThread {
                adap.addItem(msg.data.also { it?.isUploaded = true } ?: return@runOnUiThread)
                b.rvMsg.smoothScrollToPosition(0)
            }
        }
        vma.removeReplyData()
        vm.updatePersonModel(msgB.build(), tempId)
    }

    private fun handleMsgStatusAnimation(serverRec: Boolean, id: Long?) {
        val index = vm.chatMsgs.indexOfFirst { id == it.id }
        if (index == -1) return
        vm.chatMsgs[index].status = if (serverRec) MsgStatus.SENT_TO_SERVER else MsgStatus.RECEIVED
        val b = getRvViewIfVisible(index) ?: return
        val imgMsgStatus = b.findViewById<ImageView>(R.id.imgMsgStatus) ?: return
        val animDur = 200L
        lifecycleScope.launch {
            delay(animDur)
            imgMsgStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    this@ChatActivity,
                    if (serverRec) R.drawable.ic_tick else R.drawable.ic_double_tick
                )
            )
            imgMsgStatus.animate().rotationY(0f).setInterpolator(LinearInterpolator())
                .setDuration(animDur).start()
        }
        imgMsgStatus.animate().rotationY(180f).setInterpolator(LinearInterpolator())
            .setDuration(animDur).start()
    }

    //region CLICK LISTENER

    override fun onClick(v: View) {
        when (v.id) {
            b.btMenu.id -> {}
            b.btScrollToBottom.id -> b.rvMsg.smoothScrollToPosition(0)
            b.btBack.id, b.btBackSel.id, b.cardFlipper.id -> onBackPressed()
            b.btDel.id -> vma.delSelected(true)
            b.btCopy.id -> {
                if (vma.selCount.value!! > 1) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Default) {
                            val myName = Utils.myName
                            val partnerName = vm.partnerPref?.name ?: ""
                            val data = vm.chatMsgs.filter { it.id in vma.selectedMsgIds }
                                .joinToString(separator = "\n") {
                                    when (it.msgType) {
                                        MsgMediaType.TEXT, MsgMediaType.EMOJI -> "${if (it.isSent) myName else partnerName}: ${it.msg}"
                                        else -> "${if (it.isSent) myName else partnerName}: ${it.msgType.name}"
                                    }
                                }
                            withContext(Dispatchers.Main) {
                                clipText(this@ChatActivity, data, vma.selCount.value ?: 2)
                                onBackPressed()
                            }
                        }
                    }
                } else try {
                    val mod =
                        vm.chatMsgs.findLast { it.id == vma.selectedMsgIds[0] } ?: throw Exception()
                    if (mod.msgType == MsgMediaType.TEXT || mod.msgType == MsgMediaType.EMOJI)
                        clipText(this, mod.msg, 1)
                    else Toast.makeText(this, "Can't copy msg", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {

                } finally {
                    onBackPressed()
                }
            }

            b.btReply.id -> try {
                val mod =
                    vm.chatMsgs.findLast { it.id == vma.selectedMsgIds[0] } ?: throw Exception()
                vma.setReplyData(mod)
            } catch (_: Exception) {

            } finally {
                onBackPressed()
            }

            b.btCloseReplyLay.id -> {
                vma.removeReplyData()
                b.root.postDelayed({ b.etMsg.requestFocus() }, 40)
            }

            b.btGallery.id -> {
                if (!vm.canSendMsg) return
                b.btGallery.isClickable = false
                mediaPicker.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        .build()
                )
            }

            b.btAttachments.id -> {
                if (!vm.canSendMsg) return
                b.btAttachments.isClickable = false
                filePicker.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        setType("*/*")
                    }
                )
            }

            b.btSend.id -> {
                if (!vm.canSendMsg) return
                b.btSend.playAnimation()
                val msgText = b.etMsg.text.toString().trim().ifEmpty { return }

                sendTextMessage(msgText, isOnlyEmoji(msgText))
                b.etMsg.setText("")
                b.root.postDelayed({ b.etMsg.requestFocus() }, 40)
                vma.removeReplyData()
            }

            b.btImg.id -> {
                if (!vm.canSendMsg) return
                if (emojiAdap.currentList.isEmpty()) {
                    b.rvEmojiContainer.x = b.rvEmojiContainer.width.toFloat()
                    b.rvEmoji.adapter = emojiAdap
                    b.rvEmoji.setLayoutManager(
                        GridLayoutManager(
                            this,
                            3,
                            GridLayoutManager.HORIZONTAL,
                            false
                        )
                    )
                    emojiAdap.submitList(EmojisHashingUtils.emojiList)
                }
                if (b.rvEmojiContainer.x.toInt() > 0) {
                    b.rvEmojiContainer.animate()
                        .x(0F)
                        .setDuration(220)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                    return
                }

                b.rvEmojiContainer.animate()
                    .x(b.rvEmojiContainer.width.toFloat())
                    .setDuration(220)
                    .start()
            }

            b.layDetail.id -> {
                if (vma.headMenu.value == true) return
                val options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, b.imgDp, vma.phone)
                startActivityForResult(
                    Intent(this, PartnerPrefActivity::class.java)
                        .apply {
                            putExtra("phone", vma.phone)
                            putExtra("dpFile", vma.dpFile.value?.absolutePath ?: "")
                        },
                    codePrefs,
                    options.toBundle()
                )
            }
        }
    }
    //endregion CLICK LISTENER


    //region FLOW EVENTS

    private fun showEmojiViaFlow(msg: UiMsgModal) {
        if (msg.isSent) return
        if (msg.msgType != MsgMediaType.EMOJI) return
        val nameGoogleJson = EmojisHashingUtils.googleJHash[ConversionUtils.encode(msg.msg)]
        if (!nameGoogleJson.isNullOrEmpty()) {
            vmAssets.showGoogleJsonViaFlow(nameGoogleJson)
            return
        }

        val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(msg.msg)]
        if (!nameJson.isNullOrEmpty()) {
            vmAssets.showJsonViaFlow(nameJson)
            return
        }

        val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(msg.msg)]
        if (!nameGif.isNullOrEmpty()) {
            vmAssets.showGifViaFlow(nameGif)
            return
        }

        val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(msg.msg)]
        if (!nameTeleGif.isNullOrEmpty())
            vmAssets.showTeleGifViaFlow(nameTeleGif)
    }

    private fun handleFlow(msg: MsgsFlowState) {
        if (msg.fromUser != (Utils.currentPartner?.partnerId ?: "")) return
        when (msg.type) {
            FlowType.MSG -> {
                val msgL = msg.data ?: return
                if (msgL.msgType != MsgMediaType.TEXT && msgL.msgType != MsgMediaType.EMOJI)
                    vmAssets.downLoadFile(
                        msgL.msg.split(",-,")[0],
                        msgL.mediaFileName!!,
                        msgL.msgType,
                        msgL.id,
                        Utils.currentPartner!!.partnerId
                    ) {
                        msgL.isDownloaded = true
                        msgL.isProg = false
                        msgL.bytes = it
                    }
                adap.addItem(msgL)
                showEmojiViaFlow(msgL)
            }

            FlowType.UPLOAD_SUCCESS -> {
                runOnUiThread {
                    val msgT = msg.data ?: return@runOnUiThread
                    val builder = ModelMsgSocket.Builder()

                    builder.replyId(msgT.replyId)
                    builder.msgData(msgT.msg)
                    builder.replyData(msgT.rep)
                    builder.msgType(msgT.msgType)
                    builder.replyMsgType(msgT.replyType)
                    builder.isReply(msgT.isReply)
                    builder.setTimeMillis(System.currentTimeMillis())
                    builder.replyMediaFileName(msgT.replyMediaFileName)
                    builder.mediaFileName(msgT.mediaFileName)
                    builder.mediaSize(msgT.mediaSize)
                    builder.setAspectRatio(msgT.aspectRatio)
                    builder.setInfo(msgT.info)

                    vm.sendMediaUploaded(builder.build(), msgT.id, msg.fromUser)
                    vm.updatePersonModel(builder.build(), msgT.id)
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msgT.id) {
                            vm.chatMsgs[i].isUploaded = true
                            vm.chatMsgs[i].isProg = false
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            val layMediaRoot = b.findViewById<LinearLayout>(R.id.layMediaRoot)
                                ?: return@runOnUiThread
                            animateTo0(layMediaRoot)
                            b.postDelayed({ layMediaRoot.visibility = View.GONE }, 200)
                            break
                        }
                    }
                }
            }

            FlowType.UPLOAD_FAILS -> {
                runOnUiThread {
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msg.msgId) {
                            vm.chatMsgs[i].isUploaded = false
                            vm.chatMsgs[i].isProg = false
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            b.findViewById<View>(R.id.layUpload)?.visibility = View.VISIBLE
                            b.findViewById<View>(R.id.rvProg)?.visibility = View.GONE
                            break
                        }
                    }
                }
            }

            FlowType.DOWNLOAD_SUCCESS -> {
                runOnUiThread {
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msg.msgId) {
                            vm.chatMsgs[i].isDownloaded = true
                            vm.chatMsgs[i].isProg = false
                            vm.chatMsgs[i].bytes = msg.data!!.bytes
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            val layMediaRoot = b.findViewById<LinearLayout>(R.id.layMediaRoot)
                                ?: continue
                            animateTo0(layMediaRoot)
                            b.postDelayed({ layMediaRoot.visibility = View.GONE }, 200)
                            if (msg.data.msgType == MsgMediaType.FILE) continue
                            val mediaImg = b.findViewById<CorneredImageView>(R.id.media_img)
                                ?: continue
                            Glide.with(this).load(msg.data.bytes)
                                .placeholder(mediaImg.drawable)
                                .transform(RoundedCorners(12))
                                .into(mediaImg)
                            break
                        }
                    }
                }
            }

            FlowType.DOWNLOAD_FAILS -> {
                runOnUiThread {
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msg.msgId) {
                            vm.chatMsgs[i].isDownloaded = false
                            vm.chatMsgs[i].isProg = false

                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            b.findViewById<View>(R.id.layDownload)?.visibility = View.VISIBLE
                            val rvProg =
                                b.findViewById<ProgressBar>(R.id.rvProg) ?: continue

                            rvProg.animate().apply {
                                scaleX(0f)
                                scaleY(0f)
                                setDuration(200)
                                start()
                            }
                            rvProg.visibility = View.GONE
                            break
                        }
                    }
                }
            }


            FlowType.SERVER_REC -> {
                handleMsgStatusAnimation(true, msg.msgId)
            }

            FlowType.PARTNER_REC -> {
                handleMsgStatusAnimation(false, msg.msgId)
                timeVisibilityQueue.addLast(
                    Pair(
                        System.currentTimeMillis() + 2000,
                        msg.msgId ?: -1L
                    )
                )
            }

            FlowType.TYPING -> {
                if (b.tvDetails.text.toString() == "Typing...") return
                vma.setTypingText("Typing...")
                val animDur = 200L
                lifecycleScope.launch {
                    delay(animDur)
                    b.apply {
                        lottieTyping.playAnimation()
                        imgDp.visibility = View.GONE
                        lottieTyping.visibility = View.VISIBLE
                    }
                    b.cardFlipper.animate().rotationY(0f).setInterpolator(LinearInterpolator())
                        .setDuration(animDur).start()
                }
                b.cardFlipper.animate().rotationY(180f).setInterpolator(LinearInterpolator())
                    .setDuration(animDur).start()
            }

            FlowType.NO_TYPING -> {
                if (b.tvDetails.text.toString() != "Typing...") return
                if (lastSeenMillis == -1L) vma.setTypingText("Online")
                else vma.setTypingText(
                    "last seen at ${
                        ConversionUtils.getRelativeTime(
                            lastSeenMillis
                        )
                    }"
                )
                val animDur = 200L
                lifecycleScope.launch {
                    delay(animDur)
                    b.apply {
                        lottieTyping.pauseAnimation()
                        imgDp.visibility = View.VISIBLE
                        lottieTyping.visibility = View.GONE
                    }
                    b.cardFlipper.animate().rotationY(0f).setInterpolator(LinearInterpolator())
                        .setDuration(animDur).start()
                }
                b.cardFlipper.animate().rotationY(180f).setInterpolator(LinearInterpolator())
                    .setDuration(animDur).start()
            }

            FlowType.ONLINE -> {
                vma.setTypingText("Online")
                lastSeenMillis = -1L
            }

            FlowType.OFFLINE -> {
                if (b.tvDetails.text.toString().contains("Typing...")) {
                    val animDur = 200L
                    lifecycleScope.launch {
                        delay(animDur)
                        b.apply {
                            lottieTyping.pauseAnimation()
                            imgDp.visibility = View.VISIBLE
                            lottieTyping.visibility = View.GONE
                        }
                        b.cardFlipper.animate().rotationY(0f).setInterpolator(LinearInterpolator())
                            .setDuration(animDur).start()
                    }
                    b.cardFlipper.animate().rotationY(180f).setInterpolator(LinearInterpolator())
                        .setDuration(animDur).start()
                }
                lastSeenMillis = msg.msgId!!
                val relTimeText = ConversionUtils.getRelativeTime(lastSeenMillis)
                if (relTimeText.contains(':'))
                    vma.setTypingText("last seen at $relTimeText")
                else if (relTimeText == "Yesterday")
                    vma.setTypingText(
                        "last seen yesterday at ${
                            ConversionUtils.millisToTimeText(
                                lastSeenMillis
                            )
                        }"
                    )
                else vma.setTypingText("last seen on $relTimeText")
            }

            FlowType.RELOAD_RV -> {
                adap.notifyDataSetChanged()
                vm.chatMsgs.getOrNull(0)?.let { showEmojiViaFlow(it) }
            }

            FlowType.RELOAD_RV_MORE -> {
                val insertedItemsCount = (msg.msgId ?: 0L).toInt()
                val prevSize = vm.chatMsgs.size - insertedItemsCount
                adap.notifyItemRangeInserted(prevSize, insertedItemsCount)
            }

            FlowType.SET_PREFS -> {
                b.imgBg.run {
                    val mod = vm.partnerPref ?: return
                    setData(
                        isDarkModeEnabled(),
                        mod.background.alpha,
                        mod.backgroundAssetNo,
                        mod.background.color,
                        mod.background.grad
                    )
                }
                b.apply {
                    tvPartnerName.text = (vm.partnerPref?.name ?: "").ifEmpty { vma.phone }
                    if (vm.canSendMsg) return@apply

                    contRelation.visibility = View.VISIBLE
                    tvPartnerNameCard.text = tvPartnerName.text
                    Glide.with(this@ChatActivity)
                        .asBitmap()
                        .load(vma.dpFile.value)
                        .override(200)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.def_avatar)
                        .error(R.drawable.def_avatar)
                        .into(b.imgDpCard)

                    if (Utils.currentPartner!!.isConnSent) {

                        if (Utils.currentPartner!!.isRejected)
                            "Your request is rejected by user\nDo you want to send request again?"
                                .also { tvStatusCard.text = it }
                        else "Your request not Accepted by user\nDo you want to send request again?"
                            .also { tvStatusCard.text = it }
                        btNeg.visibility = View.GONE

                        btPositive.setOnClickListener {
                            vm.genKeyAndSendNotification(Utils.currentPartner!!)
                        }
                        return@apply
                    }

                    if (Utils.currentPartner!!.isRejected && Utils.currentPartner!!.isConnSent) {
                        return@apply
                    }
                    "sending you connection request\nDo you also want to connect?"
                        .also { tvStatusCard.text = it }
                    btPositive.setOnClickListener {
                        vm.acceptConnection(true)
                        contRelation.visibility = View.GONE
                    }
                    btNeg.setOnClickListener {
                        vm.acceptConnection(false)
                        contRelation.visibility = View.GONE
                        finish()
                    }
                }
            }

            FlowType.SEND_NEW_CONNECTION_REQUEST -> {
                finish()
            }

            FlowType.REQ_ACCEPTED -> {
                Utils.currentPartner?.isAccepted = true
                runOnUiThread {
                    b.contRelation.visibility = View.GONE
                    vm.openChat(msg.fromUser, vma.selectedMsgIds)
                }
            }

            FlowType.INCOMING_NEW_CONNECTION_REQUEST -> {
                Utils.currentPartner?.isAccepted = false
                vm.canSendMsg = false
                runOnUiThread {
                    b.apply {
                        contRelation.visibility = View.VISIBLE
                        Glide.with(this@ChatActivity)
                            .asBitmap()
                            .circleCrop()
                            .load(Utils.currentPartner!!.partnerId.getDpLink())
                            .into(imgDpCard)
                        tvPartnerNameCard.text = (vm.partnerPref?.name ?: "").ifEmpty { vma.phone }

                        "sending you connection request\nDo you also want to connect?"
                            .also { tvStatusCard.text = it }
                        btPositive.setOnClickListener {
                            vm.acceptConnection(true)
                            contRelation.visibility = View.GONE
                        }
                        btNeg.visibility = View.VISIBLE
                        btNeg.setOnClickListener {
                            vm.acceptConnection(false)
                            contRelation.visibility = View.GONE
                            finish()
                        }
                    }
                }
            }

            FlowType.CHANGE_GIF -> {
                runOnUiThread {
                    b.tCard.animate().apply {
                        scaleY(0f)
                        scaleX(0f)
                        setDuration(120)
                        start()
                    }
                    b.tCard.animate()
                        .scaleY(1f)
                        .scaleX(1f)
                        .setStartDelay(120)
                        .setDuration(120)
                        .start()
                    lifecycleScope.launch {
                        delay(120)
                        if (msg.data!!.msg.isEmpty()) return@launch
                        b.btImg.setAnimationFromJson(msg.data.msg, msg.data.mediaFileName)
                        b.btImg.playAnimation()
                    }
                    currentLottieEmoji = msg.data?.mediaSize ?: ""
                }
            }

            FlowType.SHOW_BIG_JSON -> {
                runOnUiThread {
                    b.bigJson.animate().scaleX(1f).scaleY(1f).setDuration(140)
                        .setInterpolator(AccelerateInterpolator()).start()
                    b.bigJson.setAnimationFromJson(msg.data!!.msg, msg.data.mediaFileName)
                    b.bigJson.playAnimation()
                }

            }

            FlowType.SHOW_BIG_GIF -> {
                runOnUiThread {
                    b.bigJson.animate().scaleX(1f).scaleY(1f).setDuration(140)
                        .setInterpolator(AccelerateInterpolator()).start()
                    Glide.with(this).load(msg.fileGif!!).skipMemoryCache(true).into(b.bigJson)
                    lifecycleScope.launch {
                        delay(3600)
                        b.bigJson.animate().scaleX(0f).scaleY(0f).setDuration(320)
                            .setInterpolator(AccelerateInterpolator()).start()
                        b.bigJson.postDelayed(
                            { Glide.with(this@ChatActivity).clear(b.bigJson) },
                            340
                        )
                    }
                }
            }

            FlowType.SET_TYPING_GIF -> {
                runOnUiThread {
                    b.lottieTyping.setAnimationFromJson(msg.data!!.msg, msg.data.mediaFileName)
                }
            }

            FlowType.OPEN_FILE -> {
                runOnUiThread {
                    try {
                        val uri: Uri = FileProvider.getUriForFile(
                            this,
                            "${applicationContext.packageName}.extProvider",
                            msg.fileGif!!
                        )

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                uri,
                                ConversionUtils.mimeTypes.getOrDefault(
                                    msg.fileGif.extension,
                                    "application/octet-stream"
                                )
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(
                            this,
                            "No application found to open this file.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            FlowType.OPEN_IMAGE, FlowType.OPEN_GIF -> {
                runOnUiThread {
                    val b = getRvViewIfPossibleForId(msgIdForImageShow) ?: return@runOnUiThread
                    val mediaImg = b.findViewById<CorneredImageView>(R.id.media_img)
                        ?: return@runOnUiThread
                    val chatMsg =
                        vm.chatMsgs.find { it.id == msgIdForImageShow } ?: return@runOnUiThread
                    GifViewActivity.bytesImage = chatMsg.bytes
                    mediaImg.transitionName = msg.fileGif?.name ?: "img"
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        mediaImg,
                        msg.fileGif?.name ?: "img"
                    )
                    if (GifViewActivity.bytesImage == null) return@runOnUiThread
                    val openActivityClass =
                        if (msg.type == FlowType.OPEN_GIF) GifViewActivity::class.java else PhotoViewActivity::class.java
                    startActivityForResult(Intent(this, openActivityClass).apply {
                        putExtra("file", msg.fileGif?.absolutePath)
                        putExtra("canSaveToGal", !chatMsg.isSent)
                        putExtra("canDelete", true)
                        if (msg.msgId == -1L)
                            putExtra("timeText", chatMsg.timeText)
                        else putExtra("timeMillis", msg.msgId)
                        putExtra(
                            "heading",
                            if (chatMsg.isSent) "You" else (vm.partnerPref?.name
                                ?: "").ifEmpty { vma.phone })
                    }, 1001, options.toBundle())
                }
            }

            FlowType.OPEN_VIDEO -> {
                runOnUiThread {
                    val b = getRvViewIfPossibleForId(msgIdForImageShow) ?: return@runOnUiThread
                    val mediaImg = b.findViewById<CorneredImageView>(R.id.media_img)
                        ?: return@runOnUiThread
                    val chatMsg =
                        vm.chatMsgs.find { it.id == msgIdForImageShow } ?: return@runOnUiThread
                    mediaImg.transitionName = msg.data?.mediaFileName ?: "img"
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        mediaImg,
                        msg.data?.mediaFileName ?: "img"
                    )
                    val openActivityClass = VideoViewActivity::class.java
                    startActivityForResult(Intent(this, openActivityClass).apply {
                        putExtra("canSaveToGal", !chatMsg.isSent)
                        putExtra("canDelete", true)
                        putExtra(
                            "heading",
                            if (chatMsg.isSent) "You" else (vm.partnerPref?.name
                                ?: "").ifEmpty { vma.phone })
                        putExtra("videoTime", msg.data?.info.toString())
                        putExtra("videoName", msg.data?.mediaFileName ?: "")
                        if (msg.msgId == -1L)
                            putExtra("timeText", chatMsg.timeText)
                        else putExtra("timeMillis", msg.msgId)
                    }, 1001, options.toBundle())
                }
            }

            else -> {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == codePrefs) {
            if (resultCode == RESULT_OK)
                vma.reloadPartnerPref()
            return
        }
        if (requestCode != 1001) return
        if (resultCode != RESULT_OK) return
        val index = vm.chatMsgs.indexOfFirst { it.id == msgIdForImageShow }
        if (index == -1) return

        vm.chatMsgs.removeAt(index)
        adap.notifyItemRemoved(index)
        vma.delMsg(msgIdForImageShow)
    }

    //endregion FLOW EVENTS

    private fun getRvViewIfVisible(pos: Int): View? {
        return ll.findViewByPosition(pos)
    }

    private fun getRvViewIfPossibleForId(msgId: Long): View? {
        for (i in vm.chatMsgs.indices) {
            if (vm.chatMsgs[i].id == msgId) {
                return ll.findViewByPosition(i) ?: return null
            }
        }
        return null
    }

    private fun isOnlyEmoji(text: String): Boolean {
        return (EmojisHashingUtils.googleJHash.containsKey(ConversionUtils.encode(text))
                || EmojisHashingUtils.jHash.containsKey(ConversionUtils.encode(text))
                || EmojisHashingUtils.gHash.containsKey(ConversionUtils.encode(text))
                || EmojisHashingUtils.teleHash.containsKey(ConversionUtils.encode(text)))
    }

    //region CLICK LISTENERS

    override fun onChatItemClicked(pos: Int, type: ClickEvents) {
        if (vma.headMenu.value == true) {
            onChatItemLongClicked(pos)
            return
        }
        when (type) {
            ClickEvents.DOWNLOAD -> onChatItemDownloadClicked(pos)
            ClickEvents.UPLOAD -> onChatItemUploadClicked(pos)
            ClickEvents.REPLY -> onChatItemReplyClicked(pos)
            ClickEvents.FILE -> openFileInAssociatedApp(pos)
            ClickEvents.IMAGE -> onImageClick(pos)
            //root Case show timer
            else -> {
                val b = getRvViewIfVisible(pos) ?: return
                val contTime = b.findViewById<LinearLayout>(R.id.contTime) ?: return
                if (vm.chatMsgs[pos].status != MsgStatus.RECEIVED) return
                contTime.visibility = View.VISIBLE
                timeVisibilityQueue.removeIf { it.second == vm.chatMsgs[pos].id }
                timeVisibilityQueue.addLast(
                    Pair(
                        System.currentTimeMillis() + 2000,
                        vm.chatMsgs[pos].id
                    )
                )
            }
        }
    }

    override fun onChatItemLongClicked(pos: Int) {
        if (!vm.canSendMsg) return
        var isSel: Boolean
        vm.chatMsgs[pos].isSelected = vma.addDelNo(vm.chatMsgs[pos].id).also { isSel = it }
        val b = getRvViewIfVisible(pos) ?: return
        val col = if (isSel) ContextCompat.getColor(this, R.color.selected)
        else ContextCompat.getColor(this, R.color.trans)
        b.setBackgroundColor(col)
    }

    override fun onOpenLinkInBrowser(link: String) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    link.toUri()
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://google.com?q=$link".toUri()
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }

    private var msgIdForImageShow = -1L
    private fun onImageClick(pos: Int) {
        if (vma.headMenu.value == true) {
            onChatItemLongClicked(pos)
            return
        }
        val mod = vm.chatMsgs.getOrNull(pos) ?: return
        msgIdForImageShow = mod.id
        if (mod.msgType == MsgMediaType.VIDEO) {
            vmAssets.openVideo(mod)
            return
        }
        vmAssets.openImage(mod.mediaFileName ?: "img", mod.msgType == MsgMediaType.GIF, mod.id)
    }

    private fun openFileInAssociatedApp(pos: Int) {
        val mod = vm.chatMsgs.getOrNull(pos) ?: return
        if (mod.msgType != MsgMediaType.FILE) {
            onChatItemClicked(pos, ClickEvents.ROOT)
            return
        }
        vmAssets.openFile(mod.mediaFileName ?: "file")
    }


    private fun animateTo0(v: View) {
        v.animate().apply {
            scaleX(0f)
            scaleY(0f)
            setDuration(200)
            start()
        }
        v.isClickable = false
    }

    private fun animateTo1(v: View) {
        v.animate().apply {
            scaleX(1f)
            scaleY(1f)
            setStartDelay(200)
            setDuration(200)
            start()
        }
        v.isClickable = true
    }

    private fun onChatItemDownloadClicked(pos: Int) {
        val mod = vm.chatMsgs[pos]
        vmAssets.downLoadFile(
            mod.msg.split(",-,")[0],
            mod.mediaFileName!!,
            mod.msgType,
            mod.id,
            Utils.currentPartner!!.partnerId
        ) {
            runOnUiThread {
                vm.chatMsgs[pos].isDownloaded = true
                vm.chatMsgs[pos].isProg = false
                vm.chatMsgs[pos].bytes = it
                val b = getRvViewIfVisible(pos) ?: return@runOnUiThread
                val rvProg = b.findViewById<ProgressBar>(R.id.rvProg) ?: return@runOnUiThread
                val mediaImg = b.findViewById<CorneredImageView>(R.id.media_img)
                    ?: return@runOnUiThread
                animateTo0(rvProg)
                Glide.with(this).load(it)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .transform(RoundedCorners(12))
                    .into(mediaImg)
            }
        }
        val b = getRvViewIfVisible(pos) ?: return
        b.findViewById<LinearLayout>(R.id.layDownload)?.visibility = View.GONE
        b.findViewById<ProgressBar>(R.id.rvProg)?.visibility = View.VISIBLE
    }

    private fun onChatItemUploadClicked(pos: Int) {
        vmAssets.uploadRetry(vm.chatMsgs[pos], Utils.currentPartner!!.partnerId)
        val b = getRvViewIfVisible(pos) ?: return
        b.findViewById<LinearLayout>(R.id.layUpload)?.visibility = View.GONE
        b.findViewById<View>(R.id.rvProg)?.visibility = View.VISIBLE
    }

    private fun onChatItemReplyClicked(posAdap: Int) {
        val mod = vm.chatMsgs[posAdap]
        val pos = vm.chatMsgs.indexOfFirst { mod.replyId == it.id }
        if (pos == -1) return

        if (replyFadeAnimator != null) replyFadeAnimator?.cancel()
        vma.replyClickID = mod.replyId
        makeReplyFadeOutAnim()
        try {
            b.rvMsg.smoothScrollToPosition(pos + 1)
        } catch (_: Exception) {
            b.rvMsg.smoothScrollToPosition(pos)
        }

    }

    private fun makeReplyFadeOutAnim() {
        replyFadeAnimator = ValueAnimator.ofInt(102, 0).apply {
            this.addUpdateListener {
                val b = getRvViewIfPossibleForId(vma.replyClickID) ?: return@addUpdateListener
                b.setBackgroundColor(Color.argb(animatedValue as Int, 124, 204, 238))
            }
            this.doOnEnd {
                val pos = vm.chatMsgs.indexOfFirst { vma.replyClickID == it.id }
                vma.replyClickID = -1L
                if (pos != -1)
                    vm.chatMsgs[pos].isSelected = false
                replyFadeAnimator = null
            }
            this.doOnCancel {
                val pos = vm.chatMsgs.indexOfFirst { vma.replyClickID == it.id }
                vma.replyClickID = -1L
                if (pos != -1)
                    vm.chatMsgs[pos].isSelected = false
                val b = getRvViewIfVisible(pos) ?: return@doOnCancel
                b.setBackgroundColor(Color.TRANSPARENT)
                replyFadeAnimator = null
            }
            this.setDuration(2800)
            this.start()
        }
    }


    override fun showRep(position: Int) {
        vma.setReplyData(vm.chatMsgs[position])
    }
    //endregion CLICK LISTENERS

    private fun powerSaveOn(): Boolean {
        val powerMan = getSystemService(POWER_SERVICE) as PowerManager
        return powerMan.isPowerSaveMode
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
            if (b.root.paddingBottom > systemBars.bottom) {
                b.root.setPadding(
                    systemBars.left, 0, systemBars.right, systemBars.bottom
                )
                val p = b.layTop.layoutParams as ConstraintLayout.LayoutParams
                p.height = p.height + systemBars.top
                b.layTop.layoutParams = p
                b.layMainHead.setPadding(0, systemBars.top, 0, 0)
                b.laySelHead.setPadding(0, systemBars.top, 0, 0)
            }
            insets
        }
    }

    private fun setPaddingsByInsets(insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        b.layBottom.translationY = -imeInsets.bottom.toFloat()
        b.btSend.translationY = -imeInsets.bottom.toFloat()
        b.btSendBG.translationY = -imeInsets.bottom.toFloat()
        b.rvEmojiContainer.translationY = -imeInsets.bottom.toFloat()
        b.btScrollToBottom.translationY = -imeInsets.bottom.toFloat()

        b.root.setPadding(
            systemBars.left,
            0,
            systemBars.right,
            if (imeInsets.bottom <= systemBars.bottom)
                systemBars.bottom - imeInsets.bottom else 0
        )
        // 🔥 Resize RecyclerView smoothly
        b.rvMsg.setPadding(
            0,
            0,
            0,
            imeInsets.bottom
        )
    }
}
