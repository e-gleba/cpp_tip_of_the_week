package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.seed.CppTipSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("C++ Tip of the Week", appName)
    }

    @Test
    fun `verify initial tips seed data covers standards`() {
        val tips = CppTipSeedData.getInitialTips()
        assertTrue(tips.isNotEmpty())
        assertTrue(tips.size >= 15)

        val standards = tips.map { it.standard }.toSet()
        assertTrue(standards.contains("C++20"))
        assertTrue(standards.contains("C++23"))
        assertTrue(standards.contains("C++26"))

        for (tip in tips) {
            assertTrue(tip.id > 0)
            assertTrue(tip.title.isNotBlank())
            assertTrue(tip.summary.isNotBlank())
            assertTrue(tip.codeExample.isNotBlank())
            assertNotNull(tip.githubUrl)
        }
    }

    @Test
    fun `verify catalog tips are properly formatted for sequential sync`() {
        val allTips = CppTipSeedData.getAllSeedTips()
        assertTrue(allTips.size >= 50)

        // Verify there are catalog tips ready for sequential sync
        val undownloaded = allTips.filter { !it.isDownloaded }
        assertTrue(undownloaded.isNotEmpty())

        for (tip in undownloaded) {
            assertTrue(tip.id > 0)
            assertTrue(tip.title.isNotBlank())
            assertTrue(tip.githubUrl.contains(tip.id.toString()))
        }
    }

    @Test
    fun `verify assets bundled tips load properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tips = com.example.data.asset.AssetTipLoader.loadAllTipsFromAssets(context)
        assertTrue("Expected bundled tips from assets, found ${tips.size}", tips.size >= 100)

        val c26 = tips.filter { it.standard == "C++26" }
        assertTrue(c26.isNotEmpty())

        val tip182 = tips.find { it.id == 182 }
        assertNotNull(tip182)
        assertTrue(tip182!!.isDownloaded)
        assertTrue(tip182.title.isNotBlank())
    }
}
