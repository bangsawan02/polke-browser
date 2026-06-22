package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crypto.CryptoHelper
import java.security.MessageDigest

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val isLoggedIn: Boolean = false,
    val encryptedToken: String = "",
    val lastSyncTime: Long = 0
) {
    val syncToken: String
        get() = CryptoHelper.decrypt(encryptedToken)

    companion object {
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            return hash.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun createSecure(email: String, passwordClear: String, token: String = ""): User {
            return User(
                email = email,
                passwordHash = hashPassword(passwordClear),
                isLoggedIn = true,
                encryptedToken = CryptoHelper.encrypt(token)
            )
        }
    }
}
