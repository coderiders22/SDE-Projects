package com.tomer.chitchat.ui.frags

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.FragmentLoginProfileBinding
import com.tomer.chitchat.utils.AlertDialogBuilder
import com.tomer.chitchat.utils.Utils.Companion.getDpLink
import com.tomer.chitchat.utils.Utils.Companion.isPermissionGranted
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragUpdateProfile : Fragment() {

    private var _binding: FragmentLoginProfileBinding? = null
    private val b get() = requireNotNull(_binding)


    private lateinit var viewModel: LoginViewModel
    private var uploadBmp: Bitmap? = null
    //region ------lifecycle----->>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginProfileBinding.inflate(inflater)
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        addObservers()
        askPermissions()
        return b.root.apply {
            b.etName.setText(viewModel.name)
            b.etName.requestFocus()
            b.btNext.setOnClickListener {
                if (b.etName.text.toString().isEmpty()) {
                    b.etName.error = "Please enter your name"
                    return@setOnClickListener
                }
                viewModel.login(b.etName.text.toString(), uploadBmp)
            }
            b.imgSelectDp.setOnClickListener {
                viewModel.showGallery(true)
            }
            Glide.with(requireActivity())
                .asBitmap()
                .load(viewModel.phone.value!!.getDpLink())
                .centerCrop()
                .circleCrop()
                .error(R.drawable.ic_add_a_photo)
                .placeholder(R.drawable.ic_add_a_photo)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            b.imgSelectDp.setPadding(0, 0, 0, 0)
                            b.imgSelectDp.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    }
                )
        }
    }

    private fun askPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModel.setStoragePermission(true)
            Dexter.withContext(requireContext()).withPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ).withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    viewModel.setStoragePermission(true)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse) {
                    lifecycleScope.launch { viewModel.flowToasts.emit("Please Provide Proper Permissions...") }
                    viewModel.setStoragePermission(false)
                    if (p0.isPermanentlyDenied)
                        showPermissionDeniedDialog("Notification")
                }

                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken) {
                    p1.continuePermissionRequest()
                }

            }).check()
            return
        }
        Dexter.withContext(requireContext()).withPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                viewModel.setStoragePermission(true)
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse) {
                lifecycleScope.launch { viewModel.flowToasts.emit("Please Provide Proper Permissions...") }
                viewModel.setStoragePermission(false)
                if (p0.isPermanentlyDenied)
                    showPermissionDeniedDialog("Storage")
            }

            override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken) {
                p1.continuePermissionRequest()
            }

        }).check()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireActivity().isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS))
                viewModel.setStoragePermission(true)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            if (requireActivity().isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE))
                viewModel.setStoragePermission(true)
    }

    private fun showPermissionDeniedDialog(permi: String) {
        AlertDialogBuilder(requireActivity())
            .setTitle("Permission Denied")
            .setDescription("$permi access has been denied permanently. You can enable it in the app settings.")
            .setPositiveButton("Go to Settings") {
                openAppSettings()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", "com.tomer.chitchat", null)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion ------lifecycle----->>>

    private fun addObservers() {
        viewModel.selectedImg.observe(viewLifecycleOwner) {
            b.apply {
                this.imgSelectDp.setPadding(0, 0, 0, 0)
                Glide.with(this@FragUpdateProfile)
                    .asBitmap()
                    .load(it)
                    .override(720)
                    .centerCrop()
                    .into(
                        object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                uploadBmp = resource
                                Glide.with(this@FragUpdateProfile)
                                    .asBitmap()
                                    .load(resource)
                                    .override(400)
                                    .centerCrop()
                                    .circleCrop()
                                    .into(b.imgSelectDp)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                        }
                    )
            }
        }
        viewModel.loginProg.observe(viewLifecycleOwner) {
            if (it) {
                b.apply {
                    b.prog.visibility = View.VISIBLE
                    b.btNext.visibility = View.GONE
                }
            } else b.apply {
                b.prog.visibility = View.GONE
                b.btNext.visibility = View.VISIBLE
            }
        }

        viewModel.storagePermission.observe(viewLifecycleOwner) {
            b.btNext.isEnabled = it
        }
    }
}