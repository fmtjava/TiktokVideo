package com.fmt.tiktokvideo.cache

import com.fmt.tiktokvideo.dao.CacheManager
import com.fmt.tiktokvideo.model.Users
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 *  用户信息缓存类
 */
object UserManager {

    private val userFlow: MutableStateFlow<Users> = MutableStateFlow(Users())
    private val needLoginOutFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     *  缓存本地用户信息
     */
    suspend fun save(profile: Users) {
        CacheManager.get().userDao.save(profile)
        needLoginOutFlow.value = false
        userFlow.emit(profile)
    }

    /**
     *  是否登录
     */
    fun isLogin(): Boolean {
        return userFlow.value.created_at.isNotEmpty()
    }

    /**
     *  获取本地缓存用户信息
     */
    suspend fun getUser(): Flow<Users> {
        loadCache()
        return userFlow
    }

    /**
     *  加载本地用户信息
     */
    suspend fun loadCache() {
        if (!isLogin()) {
            val cache = CacheManager.get().userDao.getUser()
            cache?.run {
                userFlow.emit(this)
            }
        }
    }

    /**
     *  退出登录
     */
    fun logout() {
        needLoginOutFlow.value = true
    }

    /**
     *  是否需要退出登录
     */
    fun isNeedLoginOut(): Flow<Boolean> {
        return needLoginOutFlow
    }
}