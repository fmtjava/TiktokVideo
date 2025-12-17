package com.fmt.tiktokvideo.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fmt.tiktokvideo.model.Users
import com.fmt.tiktokvideo.utils.AppGlobals

@Database(entities = [Users::class], version = 1)
abstract class CacheManager : RoomDatabase() {

    abstract val userDao: UserDao

    companion object {
        private val database = Room.databaseBuilder(
            AppGlobals.getApplication()!!,
            CacheManager::class.java,
            "tiktok_cache"
        ).build()

        @JvmStatic
        fun get(): CacheManager {
            return database
        }
    }
}