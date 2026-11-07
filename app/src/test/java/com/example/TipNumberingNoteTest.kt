package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.SettingsDialog
import com.example.ui.viewmodel.CppTipUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TipNumberingNoteTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `numbering explanation is reachable at the bottom of settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.setContent {
            MaterialTheme {
                SettingsDialog(CppTipUiState(), onDismiss = {}, onThemeSelected = {}, onCheckForUpdates = {})
            }
        }
        compose.onNodeWithTag("tip_numbering_note").performScrollTo()
            .assertTextEquals(context.getString(R.string.tip_numbering_note))
    }
}
