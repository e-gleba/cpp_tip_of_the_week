package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CppDatabase
import com.example.data.model.CppTip
import com.example.data.repository.CppTipRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CppTipRepositoryTest {
    @Test
    fun `initialization repairs a large incomplete database and preserves user data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, CppDatabase::class.java).build()
        try {
            val dao = database.cppTipDao()
            dao.insertTips((1..100).map { tip(it) })
            dao.insertTip(tip(182).copy(
                isDownloaded = false,
                isBookmarked = true,
                userNotes = "Keep my notes",
                isFeatured = true
            ))
            val repository = CppTipRepository(dao)
            repository.initializeIfNeeded(context)

            val repaired = dao.getTipByIdDirect(182)!!
            assertTrue(repaired.isDownloaded)
            assertTrue(repaired.isBookmarked)
            assertTrue(repaired.isFeatured)
            assertEquals("Keep my notes", repaired.userNotes)
            assertNotNull(dao.getTipByIdDirect(376))

            repository.initializeIfNeeded(context)
            assertEquals(repaired, dao.getTipByIdDirect(182))
        } finally {
            database.close()
        }
    }

    @Test
    fun `legacy placeholders become eligible for sync without losing notes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, CppDatabase::class.java).build()
        try {
            val dao = database.cppTipDao()
            dao.insertTip(tip(999).copy(
                codeExample = "// Tip #999: Missing\n// Full code is available online or via Sync.",
                isBookmarked = true,
                userNotes = "Keep this too"
            ))

            CppTipRepository(dao).initializeIfNeeded(context)

            val repaired = dao.getTipByIdDirect(999)!!
            assertFalse(repaired.isDownloaded)
            assertTrue(repaired.isBookmarked)
            assertEquals("Keep this too", repaired.userNotes)
        } finally {
            database.close()
        }
    }

    private fun tip(id: Int) = CppTip(
        id = id,
        title = "Tip $id",
        standard = "C++20",
        category = "Core Language",
        summary = "Test tip",
        content = "Test content",
        codeExample = "int value = 0;"
    )
}
