package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crypto.CryptoHelper

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleEncrypted: String,
    val urlEncrypted: String,
    val userId: String = "local",
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val title: String
        get() = CryptoHelper.decrypt(titleEncrypted)

    val url: String
        get() = CryptoHelper.decrypt(urlEncrypted)

    companion object {
        fun createSecure(title: String, url: String, userId: String = "local", isSynced: Boolean = false): Bookmark {
            return Bookmark(
                titleEncrypted = CryptoHelper.encrypt(title),
                urlEncrypted = CryptoHelper.encrypt(url),
                userId = userId,
                isSynced = isSynced
            )
        }
    }
}
