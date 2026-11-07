package com.example.data.repository

import android.content.Context
import com.example.data.asset.AssetTipLoader
import com.example.data.local.CppTipDao
import com.example.data.model.CppTip
import com.example.data.remote.GithubTipService
import com.example.data.remote.RateLimitException
import com.example.data.seed.CppTipSeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CppTipRepository(
    private val dao: CppTipDao,
    private val githubService: GithubTipService = GithubTipService()
) {
    suspend fun initializeIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val count = dao.getCount()
        if (count < 100) {
            val assetTips = AssetTipLoader.loadAllTipsFromAssets(context)
            if (assetTips.isNotEmpty()) {
                dao.insertTips(assetTips)
            } else {
                val allSeedTips = CppTipSeedData.getAllSeedTips()
                dao.insertTips(allSeedTips)
            }
        }
    }

    fun getAllTips(): Flow<List<CppTip>> = dao.getAllTips()

    fun getBookmarkedTips(): Flow<List<CppTip>> = dao.getBookmarkedTips()

    fun getTipById(id: Int): Flow<CppTip?> = dao.getTipById(id)

    suspend fun getTipByIdDirect(id: Int): CppTip? = withContext(Dispatchers.IO) {
        dao.getTipByIdDirect(id)
    }

    fun getFeaturedTip(): Flow<CppTip?> = dao.getFeaturedTip()

    fun searchTips(query: String): Flow<List<CppTip>> = dao.searchTips(query.trim())

    suspend fun getRandomTip(): CppTip? = withContext(Dispatchers.IO) {
        dao.getRandomTip() ?: dao.getTipByIdDirect(140)
    }

    suspend fun toggleBookmark(tipId: Int, currentBookmarked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBookmark(tipId, !currentBookmarked)
    }

    suspend fun updateNotes(tipId: Int, notes: String?) = withContext(Dispatchers.IO) {
        dao.updateNotes(tipId, notes)
    }

    /**
     * Fast single-shot sync of the full repository from GitHub archive (~200KB zip).
     * Bypasses all rate limits and batch requests.
     */
    suspend fun syncFullRepoArchive(): Result<Int> = withContext(Dispatchers.IO) {
        val result = githubService.syncFromRepoArchive()
        result.map { tips ->
            val updatedTips = tips.map { tip ->
                val existing = dao.getTipByIdDirect(tip.id)
                tip.copy(
                    isBookmarked = existing?.isBookmarked ?: false,
                    userNotes = existing?.userNotes,
                    isDownloaded = true
                )
            }
            dao.insertTips(updatedTips)
            updatedTips.size
        }
    }

    /**
     * Synchronizes tips index from tip-of-the-week.github.io/cpp/
     * without hitting REST API rate limits.
     */
    suspend fun syncCatalogFromWeb(): Result<Int> = withContext(Dispatchers.IO) {
        val result = githubService.fetchCatalogFromWeb()
        result.map { tips ->
            dao.insertTipsIgnore(tips)
            tips.size
        }
    }

    /**
     * Sequentially syncs all un-downloaded tips in the background
     * with polite pacing (e.g. 200ms) to ensure seamless offline availability.
     */
    suspend fun syncAllUndownloadedTips(
        delayMs: Long = 80,
        maxItems: Int = Int.MAX_VALUE,
        onProgress: (current: Int, total: Int, tipId: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val undownloaded = dao.getUndownloadedTips()
        if (undownloaded.isEmpty()) {
            return@withContext Result.success(0)
        }

        val toFetch = if (maxItems == Int.MAX_VALUE) undownloaded else undownloaded.take(maxItems)
        var fetchedCount = 0

        for ((index, item) in toFetch.withIndex()) {
            onProgress(index + 1, toFetch.size, item.id)

            val fetchResult = githubService.fetchTipFromWeb(item.id)
            if (fetchResult.isSuccess) {
                val fullTip = fetchResult.getOrThrow()
                val existing = dao.getTipByIdDirect(item.id)
                val toSave = fullTip.copy(
                    isBookmarked = existing?.isBookmarked ?: false,
                    userNotes = existing?.userNotes,
                    isDownloaded = true
                )
                dao.insertTip(toSave)
                fetchedCount++
            } else {
                val ex = fetchResult.exceptionOrNull()
                if (ex is RateLimitException) {
                    return@withContext Result.failure(ex)
                }
            }

            if (index < toFetch.size - 1) {
                delay(delayMs)
            }
        }

        Result.success(fetchedCount)
    }

    suspend fun fetchAndCacheTip(tipId: Int): Result<CppTip> = withContext(Dispatchers.IO) {
        val result = githubService.fetchTipFromWeb(tipId)
        result.onSuccess { fetchedTip ->
            val existing = dao.getTipByIdDirect(tipId)
            val tipToSave = fetchedTip.copy(
                isBookmarked = existing?.isBookmarked ?: false,
                userNotes = existing?.userNotes,
                isFeatured = existing?.isFeatured ?: false,
                isDownloaded = true
            )
            dao.insertTip(tipToSave)
        }
        result
    }
}
