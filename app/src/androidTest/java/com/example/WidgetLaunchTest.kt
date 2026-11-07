package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.widget.TipWidgetProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetLaunchTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun widgetRouteOpensTheRequestedTipOnColdAndWarmLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = TipWidgetProvider.tipIntent(context, 182).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitTip(182)
            scenario.onActivity { activity ->
                activity.startActivity(TipWidgetProvider.tipIntent(activity, 376))
            }
            awaitTip(376)
        }
    }

    private fun awaitTip(id: Int) {
        compose.waitUntil(15_000L) {
            compose.onAllNodesWithText("Tip #$id").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Tip #$id").assertIsDisplayed()
    }
}
