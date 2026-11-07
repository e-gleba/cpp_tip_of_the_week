package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.CppTip
import com.example.ui.screens.HomeScreen
import com.example.ui.viewmodel.CppTipUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `browsing shows tips without connectivity labels or sync progress`() {
        val tip = CppTip(182, "Example tip", "C++20", "Core Language",
            "Summary", "Content", "int value = 0;")
        compose.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = CppTipUiState(
                        filteredTips = listOf(tip, tip.copy(id = 183, isDownloaded = false)),
                        isSyncing = true,
                        syncProgressText = "Syncing test progress"
                    ),
                    onSearchChanged = {},
                    onFilterSelected = {},
                    onTipClick = {},
                    onBookmarkToggle = {}
                )
            }
        }

        compose.onNodeWithTag("tip_card_182").assertIsDisplayed()
        compose.onNodeWithTag("tip_card_183").assertIsDisplayed()
        compose.onNodeWithText("Offline", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Online", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Syncing test progress").assertDoesNotExist()
    }
}
