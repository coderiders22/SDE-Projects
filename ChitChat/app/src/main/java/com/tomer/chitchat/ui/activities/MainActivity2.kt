package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.tomer.chitchat.R
import com.tomer.chitchat.utils.getAllPossibleDetails
import com.tomer.chitchat.utils.getBmpUsingGlide
import com.tomer.chitchat.utils.getGifThumbBmpUsingGlide
import kotlinx.coroutines.launch

class MainActivity2 : AppCompatActivity() {

    private val mediaPicker: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val mimeType = contentResolver.getType(uri)
                if (mimeType != null) {
                    when {
                        mimeType.startsWith("image/gif") -> {
                            lifecycleScope.launch {
                                val bmp = getGifThumbBmpUsingGlide(uri, this@MainActivity2)
                                Log.d("TAG--", "GIF  ${bmp?.width} : ${bmp?.height}")
                                img.setImageBitmap(bmp)
                            }
                        }

                        mimeType.startsWith("video/") -> {
                            Log.d("TAG--", "video: ${getAllPossibleDetails(this,uri)}")
                            startActivity(
                                Intent(
                                    this,
                                    VideoSendPreviewActivity::class.java
                                ).apply {
                                    putExtra("uri", uri.toString())
                                    putExtra("partnerPhone", "9760877706")
                                })
                        }

                        else -> {
                            lifecycleScope.launch {
                                val bmp = getBmpUsingGlide(uri, this@MainActivity2)
                                Log.d("TAG--", "IMG ${bmp?.width} : ${bmp?.height}")
                                img.setImageBitmap(bmp)
                            }
                        }
                    }
                }
            }
        }

    private lateinit var img: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btsel).setOnClickListener {
            mediaPicker.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    .build()
            )
        }
        img = findViewById(R.id.img)
    }
}