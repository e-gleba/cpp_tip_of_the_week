package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.viewmodel.CppTipViewModel
import com.example.widget.TipWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
            try {
                awaitTip(182, scenario)
                lateinit var originalActivity: MainActivity
                scenario.onActivity { activity ->
                    originalActivity = activity
                    assertEquals("cpptotw://tip/182", activity.intent.dataString)
                    activity.startActivity(TipWidgetProvider.tipIntent(activity, 376))
                }
                awaitTip(376, scenario)
                scenario.onActivity { activity ->
                    assertSame(originalActivity, activity)
                    assertEquals("cpptotw://tip/376", activity.intent.dataString)
                }
            } finally {
                // AndroidX ActivityScenario tracks lifecycle events by the original intent.
                // onNewIntent legitimately changes the URI; restore the launch identity only
                // after the routing assertions so close() can observe activity destruction.
                scenario.onActivity { activity -> activity.intent = intent }
            }
        }
    }

    private fun awaitTip(id: Int, scenario: ActivityScenario<MainActivity>) {
        try {
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithText("Tip #$id").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Tip #$id").assertIsDisplayed()
        } catch (failure: Throwable) {
            val appState = runCatching {
                var snapshot = "Activity snapshot unavailable"
                scenario.onActivity { activity ->
                    val state = ViewModelProvider(activity)[CppTipViewModel::class.java].uiState.value
                    snapshot = "uri=${activity.intent.dataString}; action=${activity.intent.action}; " +
                        "routeHandled=${activity.intent.getBooleanExtra("widget_route_handled", false)}; " +
                        "lifecycle=${activity.lifecycle.currentState}; tips=${state.allTips.size}; " +
                        "hasRequestedTip=${state.allTips.any { it.id == id }}; " +
                        "selectedTip=${state.selectedTip?.id}; filter=${state.activeFilter}; " +
                        "loadingDetails=${state.isLoadingTipDetails}; status=${state.statusMessage}"
                }
                snapshot
            }.getOrElse { "Activity snapshot failed: $it" }
            val tree = runCatching {
                compose.onRoot(useUnmergedTree = true).printToString()
            }.getOrElse { "UI tree unavailable: $it" }
            // Put diagnostics in the assertion itself so CI's XML report contains them,
            // even when no separate logcat artifact is available. Keep the original cause.
            throw AssertionError("Expected tip #$id.\n$appState\nUnmerged UI tree:\n$tree", failure)
        }
    }
}
