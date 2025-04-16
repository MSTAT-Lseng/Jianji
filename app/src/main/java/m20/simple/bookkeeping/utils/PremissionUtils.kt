package m20.simple.bookkeeping.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


object PremissionUtils {

    val CAMERA_PERMISSION_REQUEST_CODE: Int = 100

    fun requestCameraPermission(activity: AppCompatActivity): Boolean {
        // 检查是否已拥有相机权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 直接请求权限
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
            return false
        } else {
            // 已拥有权限，可以直接使用相机
            return true
        }
    }

}