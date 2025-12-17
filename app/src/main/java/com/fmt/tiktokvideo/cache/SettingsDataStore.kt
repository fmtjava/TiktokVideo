package com.fmt.tiktokvideo.cache

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fmt.tiktokvideo.AppContext

val masterKey: MasterKey =
    MasterKey.Builder(AppContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

val encryptedPrefs = EncryptedSharedPreferences(
    AppContext,
    "secret_prefs",
    masterKey
)
