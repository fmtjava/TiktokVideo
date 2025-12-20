package com.fmt.tiktokvideo.work

import android.content.Context
import android.text.TextUtils
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.fmt.tiktokvideo.http.ApiService
import com.fmt.tiktokvideo.http.URLs
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

enum class FileType(val typeName: String) {
    IMAGE("image"),
    VIDEO("video"),
}

/**
 * 上传任务
 */
class UploadFileWorker(val appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val filePath = inputData.getString("filePath") ?: ""
        val fileType = inputData.getString("fileType") ?: ""
        if (TextUtils.isEmpty(filePath) || TextUtils.isEmpty(fileType)) {
            return Result.failure()
        } else {
            val uploadFile = File(filePath)
            val fileTypeRequestBody = fileType.toRequestBody("text/plain".toMediaTypeOrNull())
            var filePart: MultipartBody.Part? = null
            if (FileType.IMAGE.typeName == fileType) {
                filePart = MultipartBody.Part.createFormData(
                    "file",
                    uploadFile.name,
                    uploadFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
            } else if (FileType.VIDEO.typeName == fileType) {
                filePart = MultipartBody.Part.createFormData(
                    "file",
                    uploadFile.name,
                    uploadFile.asRequestBody("video/mp4".toMediaTypeOrNull())
                )
            }
            if (filePart == null) {
                return Result.failure()
            }
            // 真正进行图片上传
            val result = ApiService.get().uploadFile(URLs.UPLOAD_URL, fileTypeRequestBody, filePart)
            return if (result.success) {
                // 删除缓存的复制的文件，避免占用过多空间
                if (uploadFile.absolutePath.startsWith(appContext.cacheDir.absolutePath)) {
                    uploadFile.delete()
                }
                val fileUrl = "${URLs.BASE_UPLOAD_URL}${result.data?.fileName}"
                val outputData = Data.Builder().putString("fileUrl", fileUrl).build()
                Result.success(outputData)
            } else {
                Result.failure()
            }
        }
    }
}