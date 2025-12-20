package com.fmt.tiktokvideo.http

import android.text.TextUtils
import com.fmt.tiktokvideo.cache.Preference
import com.fmt.tiktokvideo.cache.UserManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 *  鉴权拦截器
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
        val token by Preference("api_token", "")
        if (!TextUtils.isEmpty(token)) {
            builder.header("Authorization", "Bearer $token")
        }
        val authenticatedRequest = builder.build()
        val response = chain.proceed(authenticatedRequest)
        if (response.code == 401) {
            UserManager.logout()
        }
        return response
    }
}