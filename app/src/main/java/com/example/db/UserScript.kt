package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crypto.CryptoHelper

@Entity(tableName = "user_scripts")
data class UserScript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val matchUrl: String, // e.g. "youtube.com" or "*" or globs
    val scriptContentEncrypted: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val scriptContent: String
        get() = CryptoHelper.decrypt(scriptContentEncrypted)

    companion object {
        fun createSecure(name: String, description: String, matchUrl: String, scriptContent: String, isEnabled: Boolean = true): UserScript {
            return UserScript(
                name = name,
                description = description,
                matchUrl = matchUrl,
                scriptContentEncrypted = CryptoHelper.encrypt(scriptContent),
                isEnabled = isEnabled
            )
        }
    }
}
