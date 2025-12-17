package com.fmt.tiktokvideo.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class Users(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nickname: String = "",
    val email: String = "",
    val avatar: String = "",
    val bio: String = "",
    val gender: String = "",
    val age: Int = 0,
    val location: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)