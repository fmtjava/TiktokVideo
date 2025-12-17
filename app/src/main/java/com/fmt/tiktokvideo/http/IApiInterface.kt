package com.fmt.tiktokvideo.http

import com.fmt.tiktokvideo.model.Daily
import com.fmt.tiktokvideo.model.Issue
import com.fmt.tiktokvideo.model.LoginResult
import com.fmt.tiktokvideo.model.UploadResult
import com.fmt.tiktokvideo.model.dto.LoginParam
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface IApiInterface {

    @GET("v2/feed?num=0")
    suspend fun getDaily(): Daily

    @GET
    suspend fun getDaily(@Url nextPageUrl: String): Daily

    @GET
    suspend fun getMineVideoData(@Url url: String): Issue

    @POST
    suspend fun login(@Url url: String, @Body loginParam: LoginParam): ApiResult<LoginResult>

    @Multipart
    @POST
    suspend fun uploadFile(
        @Url url: String,
        @Part("fileType") fileType: RequestBody,
        @Part file: MultipartBody.Part
    ): ApiResult<UploadResult>
}