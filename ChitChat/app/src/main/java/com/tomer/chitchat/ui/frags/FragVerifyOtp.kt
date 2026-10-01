package com.tomer.chitchat.ui.frags

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.core.view.allViews
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tomer.chitchat.databinding.FragmentVerifyOtpBinding
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FragVerifyOtp : Fragment() {

    private var _binding: FragmentVerifyOtpBinding? = null
    private val b get() = requireNotNull(_binding)

    private lateinit var clipboard: ClipboardManager
    private val clipListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboard.primaryClip
            val item = clip?.getItemAt(0)
            val copiedText = item?.text?.toString()

            Log.d("TAG--", "dfjkg: $copiedText")
            if (!copiedText.isNullOrBlank()) {
                if (copiedText.length != 6) return@OnPrimaryClipChangedListener
                if (!copiedText.isDigitsOnly()) return@OnPrimaryClipChangedListener
                for (i in 0..5) {
                    val child = b.contOtp.getChildAt(i) as EditText
                    child.setText(copiedText[i].toString())
                }
                b.contOtp.getChildAt(5).requestFocus()
            }
        }

    private lateinit var viewModel: LoginViewModel
    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentVerifyOtpBinding.inflate(inflater)
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        setTextMobile()
        setListener()
        return b.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clipboard.removePrimaryClipChangedListener(clipListener)
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        val code = StringBuilder()
        for (i in 0..5) {
            val child = b.contOtp.getChildAt(i) as EditText
            code.append(child.text)
        }
        viewModel.otpTemp = code.toString()
    }

    //endregion ------lifecycle----->>>

    private fun setListener() {
        b.textResendOTP.setOnClickListener {
            viewModel.sendOtp()
        }

        clipboard = requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        // Start listening
        clipboard.addPrimaryClipChangedListener(clipListener)

        b.buttonVerify.setOnClickListener {
            var isAnyEmpty = false
            for (i in 0..5) {
                val child = b.contOtp.getChildAt(i) as EditText
                if (child.text.isEmpty()) isAnyEmpty = true
            }
            if (isAnyEmpty) {
                Toast.makeText(requireActivity(), "Please enter valid code", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val code = StringBuilder()
            for (i in 0..5) {
                val child = b.contOtp.getChildAt(i) as EditText
                code.append(child.text)
            }
            viewModel.loginWithOtp(code.toString())
        }

        viewModel.codeSend.observe(viewLifecycleOwner) {
            b.buttonVerify.isEnabled = it
        }
        viewModel.resendOtpButton.observe(viewLifecycleOwner) {
            if (it) {
                b.textResendOTPTimer.visibility = View.GONE
                b.textResendOTP.visibility = View.VISIBLE
            } else {
                b.textResendOTPTimer.visibility = View.VISIBLE
                b.textResendOTP.visibility = View.GONE
            }
        }

        viewModel.resendOtpTimer.observe(viewLifecycleOwner) { time ->
            "Resend in $time".also { b.textResendOTPTimer.text = it }
        }

        val keyLis = View.OnKeyListener { v, keyCode, event ->
            val et = v as EditText
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
                if (et.text.toString().isEmpty()) {
                    val childIndex = b.contOtp.indexOfChild(v)
                    if (childIndex != 0) {
                        val prevChild = b.contOtp.getChildAt(childIndex - 1) as EditText
                        prevChild.requestFocus()
                        prevChild.setSelection(prevChild.text.length)
                    }

                } else et.setText("")
                return@OnKeyListener true
            }
            if (event.action == KeyEvent.ACTION_UP && arrayOf(
                    7,
                    8,
                    9,
                    10,
                    11,
                    12,
                    13,
                    14,
                    15,
                    16
                ).contains(keyCode)
            ) {
                if (et.text.toString().isEmpty()) {
                    val childIndex = b.contOtp.indexOfChild(v)
                    et.setText(('0' + keyCode - 7).toString())
                    if (childIndex != 5) {
                        val nextChild = b.contOtp.getChildAt(childIndex + 1) as EditText
                        nextChild.requestFocus()
                        nextChild.setSelection(nextChild.text.length)
                    } else et.setSelection(et.text.length)
                }
            }
            true
        }

        b.apply {
            b.contOtp.allViews.forEach { it.setOnKeyListener(keyLis) }
            b.contOtp.getChildAt(0).requestFocus()
        }

        viewModel.loginProg.observe(viewLifecycleOwner) {
            if (it) {
                b.buttonVerify.visibility = View.GONE
                b.progressBar.visibility = View.VISIBLE
            } else {
                b.buttonVerify.visibility = View.VISIBLE
                b.progressBar.visibility = View.GONE
            }
        }


    }

    /** If Intent() getStringExtra == "mobile" -> startActivity(VerifyActivity),
     * (TextView) textMobile will be received value "user mobile number" */
    fun setTextMobile() {
        b.textMobile.text = String.format("+91-%s", viewModel.phone.value.toString())
        if (viewModel.otpTemp.isNotEmpty()) {
            for (i in viewModel.otpTemp.indices) {
                val et = b.contOtp.getChildAt(i) as EditText
                et.setText(viewModel.otpTemp[i].toString())
            }
            try {
                val nextChild = b.contOtp.getChildAt(viewModel.otpTemp.length) as EditText
                b.root.postDelayed({ nextChild.requestFocus() }, 40)
            } catch (_: Exception) {
                val nextChild = b.contOtp.getChildAt(5) as EditText
                b.root.postDelayed({
                    nextChild.requestFocus()
                    nextChild.setSelection(1)
                }, 40)
            }
        }
    }
}