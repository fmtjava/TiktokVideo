package com.fmt.tiktokvideo.work

import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fmt.tiktokvideo.AppContext
import com.fmt.tiktokvideo.R
import com.fmt.tiktokvideo.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/**、
 *  文件上传管理者
 */
object UploadFileManager {

    suspend fun upload(
        originalFilePath: String,
        isVideo: Boolean,
        callback: (String?, String?) -> Unit
    ) {
        // 创建任务请求队列
        val workRequests = mutableListOf<OneTimeWorkRequest>()
        if (isVideo) {
            val coverFilePath = FileUtil.generateVideoCover(originalFilePath, 200)
            if (TextUtils.isEmpty(coverFilePath)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        AppContext, R.string.file_upload_generate_cover_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                callback(null, null)
                return
            }
            workRequests.add(getOneTimeWorkRequest(coverFilePath!!, FileType.IMAGE.typeName))
        }
        workRequests.add(
            getOneTimeWorkRequest(
                originalFilePath,
                if (isVideo) FileType.VIDEO.typeName else FileType.IMAGE.typeName
            )
        )

        // 开始执行任务
        val workContinuation = WorkManager.getInstance(AppContext).beginWith(workRequests)
        workContinuation.enqueue()

        var coverFileUploadUrl: String? = null
        var originalFileUploadUrl: String? = null

        // 监听文件上传结果
        workContinuation.workInfosLiveData.asFlow().collectLatest { workInfos ->
            var failedCount = 0
            var completedCount = 0

            for (workInfo in workInfos) {
                val state = workInfo.state
                val outputData = workInfo.outputData
                val uuid = workInfo.id

                // 上传成功事件
                if (state == WorkInfo.State.SUCCEEDED) {
                    val coverFileUploadSuccess =
                        workRequests.size == 2 && uuid == workRequests.first().id
                    val uploadUrl = outputData.getString("fileUrl")
                    if (coverFileUploadSuccess) {
                        coverFileUploadUrl = uploadUrl
                    } else {
                        originalFileUploadUrl = uploadUrl
                    }
                    completedCount++
                } else if (state == WorkInfo.State.FAILED) { // 上传失败事件
                    val coverFileUploadFail =
                        workRequests.size == 2 && uuid == workRequests.first().id
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            AppContext,
                            if (coverFileUploadFail) R.string.file_upload_cover_fail else R.string.file_upload_original_fail,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    failedCount++
                }
                // 判断上传任务数是否结束
                if (completedCount + failedCount >= workRequests.size) {
                    callback(coverFileUploadUrl, originalFileUploadUrl)
                }
            }
        }
    }

    // 获取一次性任务请求
    private fun getOneTimeWorkRequest(filePath: String, fileType: String): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<UploadFileWorker>()
            .setInputData(
                Data.Builder().putString("filePath", filePath)
                    .putString("fileType", fileType)
                    .build()
            )
            .build()
    }
}