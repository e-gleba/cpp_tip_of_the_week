package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.screens.SettingsDialog
import com.example.ui.viewmodel.CppTipUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sync progress appears in settings and disables duplicate sync`() {
        showSettings(CppTipUiState(isSyncing = true, syncProgressText = "Downloading tips..."))
        compose.onNodeWithTag("settings_resync_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("settings_sync_status").assertTextEquals("Downloading tips...")
        compose.onNodeWithText("Offline", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Online", substring = true).assertDoesNotExist()
    }

    @Test
    fun `failed sync shows an error and allows retry`() {
        var retried = false
        showSettings(CppTipUiState(statusMessage = "Sync failed. Try again.", isErrorMessage = true)) {
            retried = true
        }
        compose.onNodeWithTag("settings_sync_status").assertTextEquals("Sync failed. Try again.")
        compose.onNodeWithTag("settings_resync_button").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(retried) }
    }

    private fun showSettings(state: CppTipUiState, onSync: () -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                SettingsDialog(state, onDismiss = {}, onThemeSelected = {}, onCheckForUpdates = onSync)
            }
        }
    }
}
