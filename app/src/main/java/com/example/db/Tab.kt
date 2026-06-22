package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crypto.CryptoHelper

@Entity(tableName = "tabs")
data class Tab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleEncrypted: String,
    val urlEncrypted: String,
    val isActive: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
) {
    val title: String
        get() = CryptoHelper.decrypt(titleEncrypted).ifEmpty { "New Tab" }

    val url: String
        get() = CryptoHelper.decrypt(urlEncrypted).ifEmpty { "about:blank" }

    companion object {
        fun createSecure(title: String, url: String, isActive: Boolean = false): Tab {
            return Tab(
                titleEncrypted = CryptoHelper.encrypt(title),
                urlEncrypted = CryptoHelper.encrypt(url),
                isActive = isActive
            )
        }
    }
}
