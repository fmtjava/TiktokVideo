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
        // 目前如果 token 过期，直接退出到登录页面，真实开发场景：这里要先续 token，如果续失败再退出到登录页面
        if (response.code == 401) {
            UserManager.logout()
        }
        return response
    }
}