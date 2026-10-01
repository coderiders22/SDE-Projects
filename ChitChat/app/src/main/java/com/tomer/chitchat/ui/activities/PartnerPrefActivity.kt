package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.AccentRowBinding
import com.tomer.chitchat.databinding.ActivityPartnerPrefBinding
import com.tomer.chitchat.databinding.ItemGradBgBinding
import com.tomer.chitchat.databinding.ItemSharedContentBinding
import com.tomer.chitchat.databinding.PatternRowBinding
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.ui.views.DoodleView
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import com.tomer.chitchat.utils.Utils.Companion.isLandscapeOrientation
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.viewmodals.SettingsPartnerPrefViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@AndroidEntryPoint
class PartnerPrefActivity : AppCompatActivity(), View.OnClickListener {

    private val b by lazy { ActivityPartnerPrefBinding.inflate(layoutInflater) }
    private val vm: SettingsPartnerPrefViewModel by viewModels()


    //region LIFE CYCLE

    override fun onBackPressed() {
        if (vm.isChanged)
            setResult(RESULT_OK)
        super.onBackPressedDispatcher.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val phone = intent.getStringExtra("phone") ?: ""
        b.imgProfile.transitionName = phone
        Glide.with(this)
            .asBitmap()
            .load(File(intent.getStringExtra("dpFile") ?: ""))
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.def_avatar)
            .into(b.imgProfile)

        b.apply {
            btBack.setOnClickListener(this@PartnerPrefActivity)
            btPhone.setOnClickListener(this@PartnerPrefActivity)
            btVideo.setOnClickListener(this@PartnerPrefActivity)

            switchNoti.setOnCheckedChangeListener { _, isChecked -> vm.setNotification(isChecked) }
            switchChatLock.setOnCheckedChangeListener { _, isChecked -> vm.setChatLock(isChecked) }
            sliderDimming.addOnChangeListener { _, value, _ -> vm.setDimming(value) }
        }

        vm.transparency.observe(this) {
            b.imgBg.setTrans(it)
            b.sliderDimming.value = it
        }

        vm.partnerPref.observe(this) { mod ->
            b.apply {
                populateMessages(mod)
                tvNameBig.text = mod.name
                tvNameSmall.text = mod.name
                "+91 ${mod.phone.substring(0, 5)} ${mod.phone.substring(5)}".also {
                    tvPhone.text = it
                }
                tvAbout.text =
                    ConversionUtils.decode(mod.about.ifEmpty { "Hey+there+using+Chit+Chat%21%21%21" })
                switchNoti.isChecked = mod.notificationAllowed
                switchChatLock.isChecked = mod.chatLocked
            }
        }

        vm.sharedContent.observe(this) { list ->
            if (list.isEmpty()) {
                b.imgNoMedia.visibility = View.VISIBLE
                b.imgNoMedia.playAnimation()
                return@observe
            }
            b.imgNoMedia.visibility = View.GONE
            b.imgNoMedia.pauseAnimation()
            val size100PX = 152.px.toInt()
            val paramFirstView = LinearLayout.LayoutParams(8.px.toInt(), size100PX)
            val paramLastView = LinearLayout.LayoutParams(12.px.toInt(), size100PX)
            b.contMedia.addView(Space(this).apply { layoutParams = paramFirstView })
            val maxItems = ((window.decorView.width / size100PX) + 2).coerceAtLeast(6)
            var listNew = list
            if (list.size > maxItems) listNew = list.subList(0, maxItems)
            for (i in listNew.indices) {
                val mod = listNew[i]
                val width = (mod.third ?: 1f).times(size100PX)
                val imgParam =
                    LinearLayout.LayoutParams(width.roundToInt(), size100PX)
                        .apply { marginStart = 4.px.toInt() }
                val b1 = ItemSharedContentBinding.inflate(layoutInflater)
                b1.root.apply {
                    layoutParams = imgParam
                    scaleX = 0f
                    scaleY = 0f
                    transitionName = mod.second.name
                    setOnClickListener(mediaItemClick)
                    tag = i
                }
                if (mod.first == MsgMediaType.VIDEO) {
                    b1.imgPlayButton.visibility = View.VISIBLE
                }
//                val img = ImageView(this).apply {
//                    layoutParams = imgParam
//                    scaleX = 0f
//                    scaleY = 0f
//                    transitionName = mod.second.name
//                    setOnClickListener(mediaItemClick)
//                    tag = i
//                    background = ContextCompat.getDrawable(this@PartnerPrefActivity, R.drawable.corner_rippled_bg)
//                }
                Glide.with(b1.img)
                    .load(mod.second)
                    .error(R.drawable.round_image_24)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.round_image_24)
                    .override(size100PX)
                    .transform(RoundedCorners(24))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(b1.img)
                b.contMedia.addView(b1.root)
            }
            if (list.size > maxItems)
                b.contMedia.addView(TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(size100PX, size100PX)
                        .apply { marginStart = 4.px.toInt() }
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(this@PartnerPrefActivity, R.color.fore))
                    "+${list.size - maxItems}".also { text = it }
                    textSize = 18.px
                    typeface = ResourcesCompat.getFont(this@PartnerPrefActivity, R.font.nunito_bold)
                })

            b.contMedia.addView(Space(this).apply { layoutParams = paramLastView })
            b.contMedia.postDelayed({
                for (i in 0 until b.contMedia.childCount) {
                    b.contMedia.getChildAt(i).animate().apply {
                        scaleX(1f)
                        scaleY(1f)
                        startDelay = (i * 60L) + 100
                        duration = 140
                        interpolator = AccelerateInterpolator(1.2f)
                        start()
                    }
                }
            }, 260)
        }
        vm.loadPref(phone)
        populateThemeData()
    }

    //endregion LIFE CYCLE

    //region CLICK LISTENER]

    private val mediaItemClick = View.OnClickListener { v ->
        val pos = v.tag.toString().toInt()
        val item = vm.sharedContent.value?.getOrNull(pos) ?: return@OnClickListener
        lifecycleScope.launch {
            item.second.inputStream().use {
                GifViewActivity.bytesImage = it.readBytes()
            }
            if (GifViewActivity.bytesImage == null || (GifViewActivity.bytesImage?.size
                    ?: 0) < 10
            ) return@launch
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this@PartnerPrefActivity,
                v,
                v.transitionName
            )
            val viewerClass = if (item.first == MsgMediaType.GIF) GifViewActivity::class.java
            else if (item.first == MsgMediaType.VIDEO) VideoViewActivity::class.java
            else PhotoViewActivity::class.java
            startActivity(Intent(this@PartnerPrefActivity, viewerClass).apply {
                putExtra("file", item.second.absolutePath)
                putExtra("canSaveToGal", true)
                putExtra("canDelete", false)
                putExtra("timeText", "")
                putExtra("heading", item.second.name)
            }, options.toBundle())
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            b.btBack.id -> onBackPressed()
            b.btPhone.id ->
                startActivity(
                    Intent(this, CallingActivity::class.java)
                        .apply {
                            putExtra("video", false)
                            putExtra("phoneNo", vm.partnerPref.value?.phone)
                            putExtra("isCaller", true)
                        }
                )

            b.btVideo.id ->
                startActivity(
                    Intent(this, CallingActivity::class.java)
                        .apply {
                            putExtra("video", true)
                            putExtra("phoneNo", vm.partnerPref.value?.phone)
                            putExtra("isCaller", true)
                        }
                )
        }
    }
    //endregion CLICK LISTENER

    //region UI MSG

    private val clickDoodle = View.OnClickListener { v ->
        val br = PatternRowBinding.bind(v)
        for (i in 1..10)
            ((b.contBgDoodles.getChildAt(i) as CardView).getChildAt(0) as ConstraintLayout).getChildAt(
                1
            ).visibility = View.GONE
        br.selectionView.visibility = View.VISIBLE
        vm.setBackGround(
            v.tag.toString().toInt(),
            vm.rvDoodle.find { it.first.toString() == v.tag.toString() }?.second
        )
    }

    private val clickAccent = View.OnClickListener { v ->
        vm.setAccent(v.tag.toString().toInt())
    }

    private val clickBgRender = View.OnClickListener { v ->
        vm.setBackGroundIndex(v.tag.toString().toInt())
    }

    private fun populateThemeData() {
        val dark = isDarkModeEnabled()
        val paramFirstView = LinearLayout.LayoutParams(18.px.toInt(), 100)
        val paramLastView = LinearLayout.LayoutParams(22.px.toInt(), 100)
        b.contBgDoodles.addView(Space(this).apply { layoutParams = paramFirstView })
        vm.rvDoodle.forEach { pair ->
            val bR = PatternRowBinding.bind(
                LayoutInflater.from(this).inflate(R.layout.pattern_row, b.contBgDoodles, false)
            )
            ((bR.root.getChildAt(0) as ConstraintLayout).getChildAt(0) as DoodleView)
                .setData(dark, 1f, pair.first, pair.second.color, pair.second.grad)
            bR.root.setOnClickListener(clickDoodle)
            bR.root.tag = pair.first.toString()
            b.contBgDoodles.addView(bR.root)
        }
        b.contBgDoodles.addView(Space(this).apply { layoutParams = paramLastView })
        b.root.postDelayed({
            val assetNo = vm.partnerPref.value?.backgroundAssetNo ?: 7
            ((b.contBgDoodles.getChildAt(assetNo) as CardView).getChildAt(0) as ConstraintLayout).getChildAt(
                1
            ).visibility = View.VISIBLE
        }, 100)


        val roundCorner = 52.px
        b.contBgGrades.addView(Space(this).apply { layoutParams = paramFirstView })
        for (i in vm.rvBgRenders.indices step 2) {

            val bR = ItemGradBgBinding.bind(
                LayoutInflater.from(this).inflate(R.layout.item_grad_bg, b.contBgGrades, false)
            )
            val mod1 = vm.rvBgRenders[i]
            val mod2 = vm.rvBgRenders[i + 1]
            if (mod1.grad == null)
                bR.v1.setData(null, roundCorner, mod1.color)
            else bR.v1.setData(null, roundCorner, mod1.grad)

            if (mod2.grad == null)
                bR.v2.setData(null, roundCorner, mod2.color)
            else bR.v2.setData(null, roundCorner, mod2.grad)
            bR.v1.setOnClickListener(clickBgRender)
            bR.v1.tag = i.toString()

            bR.v2.setOnClickListener(clickBgRender)
            bR.v2.tag = (i + 1).toString()
            b.contBgGrades.addView(bR.root)
        }
        b.contBgGrades.addView(Space(this).apply { layoutParams = paramLastView })

        val colBg = ContextCompat.getColor(this, R.color.softBg)
        val corners = vm.myPref.msgItemCorners.px.times(.78f)
        b.contAccents.addView(Space(this).apply { layoutParams = paramFirstView })
        vm.rvAccent.forEachIndexed { i, mod ->
            val bR = AccentRowBinding.bind(
                LayoutInflater.from(this).inflate(R.layout.accent_row, b.contAccents, false)
            )

            bR.bgView2.setData(true, corners, colBg)
            if (mod.grad == null)
                bR.bgView.setData(false, corners, mod.color)
            else bR.bgView.setData(false, corners, mod.grad)
            bR.root.setOnClickListener(clickAccent)
            bR.root.tag = i.toString()
            b.contAccents.addView(bR.root)
        }
        b.contAccents.addView(Space(this).apply { layoutParams = paramLastView })
    }


    private fun populateMessages(mod: ModelPartnerPref) {
        b.imgBg.setData(
            isDarkModeEnabled(),
            mod.background.alpha,
            mod.backgroundAssetNo,
            mod.background.color,
            mod.background.grad
        )
        if (mod.accent.grad == null) b.accentSend.setData(null, 40.px, mod.accent.color)
        else b.accentSend.setData(null, 40.px, mod.accent.grad!!)

        val b1 = b.item1
        val b2 = b.item2

        val params1 = b1.msgLay.layoutParams as ConstraintLayout.LayoutParams
        params1.horizontalBias = 0f
        b1.msgLay.setLayoutParams(params1)
        b1.innerLay.gravity = Gravity.START
        b1.msgLay.gravity = Gravity.START
        b1.msgBg.foreground = ContextCompat.getDrawable(this, R.drawable.msg_rec)
        b1.RepTv.setGravity(Gravity.START)
        b1.msgTv.setTextColor(ContextCompat.getColor(this, R.color.white))
        b1.contTime.gravity = Gravity.START
        b1.imgMsgStatus.visibility = View.GONE
        if (mod.accent.grad == null)
            b1.msgBg.setData(false, vm.myPref.msgItemCorners.px, mod.accent.color)
        else b1.msgBg.setData(false, vm.myPref.msgItemCorners.px, mod.accent.grad!!)

        "Change accent color or gradient".also { b1.msgTv.text = it }
        b1.emojiTv.visibility = View.GONE
        "Hii there!".also { b1.RepTv.text = it }


        val params = b2.msgLay.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.horizontalBias = 1f
        b2.msgLay.setLayoutParams(params)
        b2.innerLay.gravity = Gravity.END
        b2.msgLay.gravity = Gravity.END
        b2.msgBg.foreground = ContextCompat.getDrawable(this, R.drawable.msg_sent)
        b2.RepTv.setGravity(Gravity.END)
        b2.msgTv.setTextColor(ContextCompat.getColor(this, R.color.fore))
        b2.contTime.gravity = Gravity.END
        b2.imgMsgStatus.visibility = View.GONE
        b2.msgBg.setData(
            true,
            vm.myPref.msgItemCorners.px,
            ContextCompat.getColor(this, R.color.softBg)
        )

        "Change background and pattern".also { b2.msgTv.text = it }
        b2.emojiTv.visibility = View.GONE
        b2.RepTv.visibility = View.GONE


        b1.repImgRv.visibility = View.GONE
        b2.repImgRv.visibility = View.GONE
        b1.mediaCont.visibility = View.GONE
        b1.imgFileType.visibility = View.GONE
        b2.mediaCont.visibility = View.GONE
        b2.imgFileType.visibility = View.GONE
        b2.imgFileType.visibility = View.GONE
    }

    //endregion UI MSG
}