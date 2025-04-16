package m20.simple.bookkeeping.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class FileUtils (private val context: Context) {

    private val dataDir = context.dataDir
    private val filesDir = context.filesDir
    private val cacheDir = context.cacheDir

    private val photosDirName = "Photos"

    fun createDirectory(name: String, fileObject: File): Boolean {
        val photosDir = File(fileObject, name)
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        return photosDir.exists()
    }

    // 获取文件扩展名，传入文件名（不含点号）
    fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex <= 0 || lastDotIndex == fileName.length - 1) {
            ""
        } else {
            fileName.substring(lastDotIndex + 1).lowercase()
        }
    }

    fun storePhotosFromUri(uri: Uri): String? {
        // 获取文件扩展名，默认为 "jpg"
        val fileExtension = context.contentResolver.query(uri, null, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                        ?.let { getFileExtension(it) }
                } else null
            } ?: "jpg"

        // 生成文件名
        val fileName = buildString {
            append(TimeUtils.getTimestamp())
            append("_")
            append(Random.nextInt(0, 1000000000).toString().padStart(10, '0'))
            append(".")
            append(fileExtension)
        }

        // 创建 Photos 文件夹
        if (!createDirectory(photosDirName, filesDir)){
            return null
        }

        // 创建目标文件
        val photosDir = File(context.filesDir, photosDirName)
        val destinationFile = File(photosDir, fileName)
        try {
            // 从 Uri 读取数据并写入文件
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return fileName
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deletePhotos(name: String): Boolean {
        val photosDir = File(filesDir, photosDirName)
        return File(photosDir, name).takeIf { it.exists() }?.delete() ?: false
    }

}