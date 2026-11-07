package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CppTip
import kotlinx.coroutines.flow.Flow

@Dao
interface CppTipDao {
    @Query("SELECT * FROM cpp_tips ORDER BY id ASC")
    fun getAllTips(): Flow<List<CppTip>>

    @Query("SELECT * FROM cpp_tips WHERE isBookmarked = 1 ORDER BY id ASC")
    fun getBookmarkedTips(): Flow<List<CppTip>>

    @Query("SELECT * FROM cpp_tips WHERE id = :id LIMIT 1")
    fun getTipById(id: Int): Flow<CppTip?>

    @Query("SELECT * FROM cpp_tips WHERE id = :id LIMIT 1")
    suspend fun getTipByIdDirect(id: Int): CppTip?

    @Query("SELECT * FROM cpp_tips WHERE isFeatured = 1 LIMIT 1")
    fun getFeaturedTip(): Flow<CppTip?>

    @Query("""
        SELECT * FROM cpp_tips 
        WHERE (title LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR standard LIKE '%' || :query || '%'
           OR CAST(id AS TEXT) = :query)
        ORDER BY id ASC
    """)
    fun searchTips(query: String): Flow<List<CppTip>>

    @Query("SELECT * FROM cpp_tips WHERE isDownloaded = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomTip(): CppTip?

    @Query("SELECT * FROM cpp_tips WHERE isDownloaded = 0 ORDER BY id ASC")
    suspend fun getUndownloadedTips(): List<CppTip>

    @Query("SELECT id FROM cpp_tips WHERE isDownloaded = 1")
    suspend fun getDownloadedIds(): List<Int>

    // Older asset imports incorrectly marked this exact placeholder as downloaded.
    @Query("UPDATE cpp_tips SET isDownloaded = 0 WHERE isDownloaded = 1 AND codeExample LIKE '%// Full code is available online or via Sync.'")
    suspend fun repairAssetPlaceholders()

    @Transaction
    suspend fun importBundledTips(tips: List<CppTip>) {
        insertTipsIgnore(tips)
        for (tip in tips) {
            if (!tip.isDownloaded) continue
            val existing = getTipByIdDirect(tip.id) ?: continue
            if (!existing.isDownloaded) {
                updateTip(tip.copy(
                    isBookmarked = existing.isBookmarked,
                    userNotes = existing.userNotes,
                    isFeatured = existing.isFeatured
                ))
            }
        }
    }

    @Query("SELECT COUNT(*) FROM cpp_tips")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM cpp_tips")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cpp_tips WHERE isDownloaded = 1")
    fun getDownloadedCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTips(tips: List<CppTip>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTipsIgnore(tips: List<CppTip>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: CppTip)

    @Update
    suspend fun updateTip(tip: CppTip)

    @Query("UPDATE cpp_tips SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, isBookmarked: Boolean)

    @Query("UPDATE cpp_tips SET userNotes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Int, notes: String?)
}
