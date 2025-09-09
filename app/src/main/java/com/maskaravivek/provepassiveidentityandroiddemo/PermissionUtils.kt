package com.maskaravivek.provepassiveidentityandroiddemo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    const val PHONE_STATE_PERMISSION_REQUEST_CODE = 1001
    
    private const val REQUIRED_PERMISSION = Manifest.permission.READ_PHONE_STATE
    
    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestPhoneStatePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(REQUIRED_PERMISSION),
            PHONE_STATE_PERMISSION_REQUEST_CODE
        )
    }
    
    fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            REQUIRED_PERMISSION
        )
    }
    
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (requestCode == PHONE_STATE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }
}