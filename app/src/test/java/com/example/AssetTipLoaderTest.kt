package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.asset.AssetTipLoader
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssetTipLoaderTest {
    @Test
    fun `only bundled markdown files are marked downloaded`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileIds = context.assets.list("tips").orEmpty()
            .filter { it.endsWith(".md") }
            .mapNotNull { it.removeSuffix(".md").toIntOrNull() }
            .toSet()
        val tips = AssetTipLoader.loadAllTipsFromAssets(context)

        assertEquals(fileIds, tips.filter { it.isDownloaded }.map { it.id }.toSet())
        assertEquals(tips.size, tips.map { it.id }.toSet().size)
    }
}
