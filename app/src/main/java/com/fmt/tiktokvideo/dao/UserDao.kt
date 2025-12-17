package com.fmt.tiktokvideo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fmt.tiktokvideo.model.Users

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: Users): Long // 返回自增 ID

    @Query("select * from users limit 1") // 只查询一条用户数据
    suspend fun getUser(): Users?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(profile: Users): Int // 返回影响行数

    @Delete
    suspend fun delete(profile: Users): Int // 返回影响行数
}