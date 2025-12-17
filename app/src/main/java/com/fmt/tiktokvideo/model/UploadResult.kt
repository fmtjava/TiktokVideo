package com.fmt.tiktokvideo.model

class UploadResult(
    val id: Int,
    val fileName: String,
    val originalName: String,
    val size: Long,
    val mimeType: String,
    val fileType: String,
    val uploader: Uploader,
    val uploadTime: String
)

data class Uploader(
    val account_id: Int,
    val email: String,
    val nickname: String,
    val avatar: String,
    val bio: String,
    val gender: String,
    val age: Int,
    val location: String,
    val created_at: String,
    val updated_at: String
)