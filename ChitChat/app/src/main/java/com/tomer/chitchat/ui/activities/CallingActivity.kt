package com.tomer.chitchat.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.tomer.chitchat.databinding.ActivityCallingBinding

class CallingActivity : AppCompatActivity() {

    private val b by lazy { ActivityCallingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(b.root)


    }
}
