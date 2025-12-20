package com.fmt.tiktokvideo.cache

import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fmt.tiktokvideo.AppContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val masterKey: MasterKey =
    MasterKey.Builder(AppContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

val encryptedPrefs = EncryptedSharedPreferences(
    AppContext,
    "secret_prefs",
    masterKey
)

class Preference<T>(val name: String, val default: T) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return findPreference(name)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putPreference(name, value)
    }

    private fun findPreference(key: String): T {
        return when (default) {
            is Long -> encryptedPrefs.getLong(key, default)
            is Int -> encryptedPrefs.getInt(key, default)
            is Float -> encryptedPrefs.getFloat(key, default)
            is String -> encryptedPrefs.getString(key, default)
            is Boolean -> encryptedPrefs.getBoolean(key, default)
            else -> throw IllegalStateException("Unsupported type")
        } as T
    }

    private fun putPreference(key: String, value: T) {
        encryptedPrefs.edit {
            when (value) {
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                else -> throw IllegalStateException("Unsupported type")
            }
        }
    }
}
