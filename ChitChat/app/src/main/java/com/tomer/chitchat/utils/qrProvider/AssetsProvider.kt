package com.tomer.chitchat.utils.qrProvider

import android.graphics.Color
import com.tomer.chitchat.R

object AssetsProvider {
    val trailAssets by lazy {
        mapOf(
            1 to R.drawable.q_trail1,
            2 to R.drawable.q_trail2,
            3 to R.drawable.q_trail3,
            4 to R.drawable.q_trail4,
            5 to R.drawable.q_trail5,
        )
    }
    val cornerAssets by lazy {
        mapOf(
            1 to R.drawable.q_corner1,
            2 to R.drawable.q_corner2,
        )
    }
    val aloneAssets by lazy {
        mapOf(
            1 to R.drawable.q_alone1,
            2 to R.drawable.q_alone2,
            3 to R.drawable.q_alone3,
        )
    }
    val eyesAssets by lazy {
        mapOf(
            1 to R.drawable.q_eye_1,
            2 to R.drawable.q_eye_2,
            3 to R.drawable.q_eye_3,
            4 to R.drawable.q_eye_4,
            5 to R.drawable.q_eye_5,
            6 to R.drawable.q_eye_6,
        )
    }

    //alone,corner,trail,eye
    val bodyType by lazy {
        mapOf(
            1 to listOf(1,1,5,4),
            2 to listOf(1,2,1,3),
            3 to listOf(3,2,4,6),
            4 to listOf(2,2,2,6),
            5 to listOf(2,2,2,5),
            6 to listOf(2,2,3,1),
        )
    }

    //angle,start,end
    val gradType by lazy {
        mapOf(
            1 to GradModel(120,Color.parseColor("#a6c0fe"),Color.parseColor("#f68084")),
            2 to GradModel(30,Color.parseColor("#43e97b"),Color.parseColor("#38f9d7")),
            3 to GradModel(90,Color.parseColor("#642b73"),Color.parseColor("#c6426e")),
            4 to GradModel(45,Color.parseColor("#cb356b"),Color.parseColor("#bd3f32")),
            5 to GradModel(125,Color.parseColor("#283c86"),Color.parseColor("#45a247")),
            6 to GradModel(15,Color.parseColor("#ff0844"),Color.parseColor("#ffb199")),
            7 to GradModel(235,Color.parseColor("#0fd850"),Color.parseColor("#f9f047")),
            8 to GradModel(180,Color.parseColor("#9be15d"),Color.parseColor("#00e3ae")),
        )
    }
}
