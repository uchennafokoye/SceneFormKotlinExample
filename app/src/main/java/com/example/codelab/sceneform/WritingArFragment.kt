package com.example.codelab.sceneform

import android.Manifest
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Environment
import com.google.ar.sceneform.ux.ArFragment
import java.io.File.separator
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException


class WritingArFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String?> {
        val additionalPermissions = super.getAdditionalPermissions()
        val permissionLength = additionalPermissions?.size ?: 0
        val permissions = arrayOfNulls<String>(permissionLength + 1)
        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (permissionLength > 0) {
            System.arraycopy(additionalPermissions!!, 0, permissions, 1, additionalPermissions.size)
        }
        return permissions
    }


}