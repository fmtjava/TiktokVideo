package com.fmt.tiktokvideo.http

class ApiResult<T> {
    internal val code: Int = 0
    val success
        get() = code == 200
    val message: String = ""
    val data: T? = null
}