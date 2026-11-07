package com.example

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 34])
class WidgetMetadataTest {
    @Test
    fun `widget metadata retains fallbacks and selects supported attributes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val android = "http://schemas.android.com/apk/res/android"
        context.resources.getXml(R.xml.tip_widget_info).use { parser ->
            while (parser.next() != XmlPullParser.START_TAG) {
                check(parser.eventType != XmlPullParser.END_DOCUMENT)
            }
            assertEquals("appwidget-provider", parser.name)
            assertNotNull(parser.getAttributeValue(android, "minWidth"))
            assertNotNull(parser.getAttributeValue(android, "minHeight"))
            assertEquals(86400000, parser.getAttributeIntValue(android, "updatePeriodMillis", 0))
            assertEquals(R.layout.tip_widget, parser.getAttributeResourceValue(android, "initialLayout", 0))
            val modern = Build.VERSION.SDK_INT >= 31
            assertEquals(if (modern) 3 else 0, parser.getAttributeIntValue(android, "targetCellWidth", 0))
            assertEquals(if (modern) 2 else 0, parser.getAttributeIntValue(android, "targetCellHeight", 0))
            assertEquals(if (modern) R.layout.tip_widget else 0,
                parser.getAttributeResourceValue(android, "previewLayout", 0))
        }
    }
}
