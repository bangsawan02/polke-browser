package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    
    // --- BOOKMARKS ---
    @Query("SELECT * FROM bookmarks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE id = :id AND isDeleted = 0")
    suspend fun getBookmarkById(id: Long): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun hardDeleteBookmark(id: Long)

    @Query("UPDATE bookmarks SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteBookmark(id: Long)

    // --- TABS ---
    @Query("SELECT * FROM tabs ORDER BY lastAccessed DESC")
    fun getAllTabs(): Flow<List<Tab>>

    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTab(): Tab?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: Tab): Long

    @Update
    suspend fun updateTab(tab: Tab)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTab(id: Long)

    @Query("UPDATE tabs SET isActive = 0")
    suspend fun deactivateAllTabs()

    // --- USER SCRIPTS (TAMPERMONKEY) ---
    @Query("SELECT * FROM user_scripts ORDER BY createdAt DESC")
    fun getAllScripts(): Flow<List<UserScript>>

    @Query("SELECT * FROM user_scripts WHERE isEnabled = 1")
    suspend fun getActiveScripts(): List<UserScript>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: UserScript): Long

    @Delete
    suspend fun deleteScript(script: UserScript)

    // --- USERS ---
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getPrimaryUser(): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    // --- BROWSER HISTORY ---
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    // --- SHORTCUTS ---
    @Query("SELECT * FROM shortcuts ORDER BY id ASC")
    fun getAllShortcuts(): Flow<List<ShortcutItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(item: ShortcutItem): Long

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteShortcutItem(id: Long)

    @Query("DELETE FROM shortcuts")
    suspend fun clearAllShortcuts()
}
