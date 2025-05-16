package m20.simple.bookkeeping.api.backup

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object BackupCreator {

    init {
        System.loadLibrary("jjnative")
    }

    external fun getAppSignature(context: Context, packageName: String = context.packageName, algorithm: String = "SHA-256"): String?

}