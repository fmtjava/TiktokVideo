package com.fmt.tiktokvideo.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

internal object FileUtil {

    /**
     *  生成视频封面
     */
    fun generateVideoCover(videoFilePath: String, limit: Int): String? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoFilePath)
        val bitmap = retriever.frameAtTime
        val application = AppGlobals.getApplication()
        if (bitmap != null && application != null) {
            // 先压缩封面图片
            val bytes = compressBitmap(bitmap, limit)
            val file = File(application.cacheDir, "${System.currentTimeMillis()}.jpeg")
            // 再写入本地文件中
            try {
                file.createNewFile()
                FileOutputStream(file).use {
                    it.write(bytes)
                    it.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
            return file.absolutePath
        }
        return null
    }

    /**
     *  压缩图片
     */
    private fun compressBitmap(bitmap: Bitmap, limit: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        var option = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, option, baos)
        if (limit > 0) {
            while (baos.toByteArray().size > limit * 1024) {
                baos.reset()
                option -= 5
                bitmap.compress(Bitmap.CompressFormat.JPEG, option, baos)
            }
        }
        val bytes = baos.toByteArray()
        try {
            baos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bytes
    }
}