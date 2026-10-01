package com.tomer.chitchat.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.DiaActionBinding

class AlertDialogBuilder(private val context: Context) {

    private var title: String? = null
    private var description: String? = null
    private var positiveButtonText: String? = null
    private var negativeButtonText = "Cancel"
    private var positiveButtonAction: (() -> Unit)? = null
    private var negativeButtonAction: (() -> Unit)? = null
    private var cancelAction: (() -> Unit)? = null
    private var cancelAble = true

    private var currentDialog: AlertDialog? = null

    fun setTitle(title: String): AlertDialogBuilder {
        this.title = title
        return this
    }

    fun setDescription(description: String): AlertDialogBuilder {
        this.description = description
        return this
    }

    fun setPositiveButton(text: String, action: (() -> Unit)? = null): AlertDialogBuilder {
        this.positiveButtonText = text
        this.positiveButtonAction = action
        return this
    }

    fun setNegativeButton(text: String, action: (() -> Unit)? = null): AlertDialogBuilder {
        this.negativeButtonText = text
        this.negativeButtonAction = action
        return this
    }

    fun setOnCancelListener(action: (() -> Unit)? = null): AlertDialogBuilder {
        this.cancelAction = action
        return this
    }

    fun setCancelable(cancelable: Boolean): AlertDialogBuilder {
        this.cancelAble = cancelable
        return this
    }

    fun build(): AlertDialog {
        return currentDialog ?: privateBuild()
    }

    private fun privateBuild(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val b = DiaActionBinding.inflate(LayoutInflater.from(context))
        builder.setCancelable(cancelAble)
        builder.setView(b.root)
        val dialog = builder.create()

        title?.let {
            b.tvTitle.text = it
            b.tvTitle.visibility = View.VISIBLE
        }
        description?.let {
            b.tvDes.text = it
            b.tvDes.visibility = View.VISIBLE
        }
        positiveButtonText?.let {
            b.btPositive.text = it
            b.btPositive.visibility = View.VISIBLE
            b.btPositive.setOnClickListener {
                positiveButtonAction?.invoke()
                dialog.dismiss()
            }
        }
        b.btCancel.setOnClickListener { dialog.cancel() }
        b.btCancel.text = negativeButtonText

        dialog.setOnCancelListener { cancelAction?.invoke() }
        dialog.window?.attributes?.windowAnimations = R.style.Dialog
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = dialog
        return dialog
    }

    fun show() {
        build().show()
    }
}