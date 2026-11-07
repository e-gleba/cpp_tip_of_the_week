package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CppDatabase
import com.example.data.model.CppTip
import com.example.data.model.WidgetTip
import com.example.widget.TipWidgetProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TipWidgetTest {
    @Test
    fun `daily selection is stable and wraps without assuming consecutive IDs`() {
        val tips = listOf(WidgetTip(182, "First", "C++20"), WidgetTip(376, "Last", "C++26"))
        assertEquals(tips[0], TipWidgetProvider.selectTip(tips, 0))
        assertEquals(tips[1], TipWidgetProvider.selectTip(tips, 1))
        assertEquals(tips[0], TipWidgetProvider.selectTip(tips, 2))
        assertNull(TipWidgetProvider.selectTip(emptyList(), 0))
    }

    @Test
    fun `tap targets the displayed tip and reuses the existing activity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = TipWidgetProvider.tipIntent(context, 376)
        assertEquals(MainActivity::class.java.name, intent.component!!.className)
        assertEquals(376, TipWidgetProvider.tipIdFromIntent(intent))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertNull(TipWidgetProvider.tipIdFromIntent(TipWidgetProvider.tipIntent(context, null)))
    }

    @Test
    fun `invalid widget routes are rejected`() {
        listOf("https://tip/182", "cpptotw://other/182", "cpptotw://tip/0",
            "cpptotw://tip/nope", "cpptotw://tip/182/extra").forEach { uri ->
            assertNull(TipWidgetProvider.tipIdFromIntent(Intent(Intent.ACTION_VIEW, Uri.parse(uri))))
        }
    }

    @Test
    fun `widget query returns only downloaded tip metadata in ID order`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, CppDatabase::class.java).build()
        try {
            database.cppTipDao().insertTips(listOf(
                tip(376), tip(200).copy(isDownloaded = false), tip(182)
            ))
            assertEquals(listOf(182, 376), database.cppTipDao().getWidgetTips().map { it.id })
        } finally {
            database.close()
        }
    }

    private fun tip(id: Int) = CppTip(id, "Tip $id", "C++20", "Core Language",
        "Summary", "Content", "int value = 0;")
}
