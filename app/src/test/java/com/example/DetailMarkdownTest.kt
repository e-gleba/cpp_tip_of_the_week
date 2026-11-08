package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.asset.AssetTipLoader
import com.example.data.model.CppTip
import com.example.ui.components.TipMarkdownParser
import com.example.ui.components.markdownChildren
import com.example.ui.screens.DetailScreen
import org.commonmark.node.FencedCodeBlock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetailMarkdownTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `stacktrace screen renders every original code block and the final solution link`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val markdown = context.assets.open("tips/295.md").bufferedReader().use { it.readText() }
        val tip = AssetTipLoader.parseMarkdownToTip(295, markdown).copy(
            summary = "LEGACY SUMMARY MUST NOT REPEAT",
            codeExample = "LEGACY EXTRACTED EXAMPLE",
            puzzleSnippet = "LEGACY INFERRED PUZZLE",
            puzzleAnswer = "Check Godbolt compiler output to solve the puzzle."
        )
        showTip(tip)
        val blocks = TipMarkdownParser.parse(markdown).markdownChildren()
        assertEquals(14, blocks.count { it is FencedCodeBlock })
        blocks.forEachIndexed { index, block ->
            if (block is FencedCodeBlock) {
                // The downloaded article has one badge item before its Markdown blocks.
                compose.onNodeWithTag("detail_scroll_column").performScrollToIndex(index + 1)
                compose.onNode(
                    hasTestTag("code_block_text") and hasAnyAncestor(hasTestTag("tip_markdown_$index")),
                    useUnmergedTree = true
                ).assertTextEquals(block.literal)
            }
        }
        val lastLink = "https://godbolt.org/z/oTWz4h6z4"
        compose.onNodeWithTag("detail_scroll_column").performScrollToNode(hasText(lastLink))
        compose.onNodeWithText(lastLink).assertIsDisplayed()
        compose.onNodeWithText("Tip #295 Code").assertDoesNotExist()
        compose.onNodeWithText("LEGACY EXTRACTED EXAMPLE").assertDoesNotExist()
        compose.onNodeWithText("LEGACY INFERRED PUZZLE").assertDoesNotExist()
        compose.onNodeWithText("Check Godbolt compiler output to solve the puzzle.").assertDoesNotExist()
    }

    @Test
    fun `empty article retains its summary and code fallback`() {
        showTip(CppTip(182, "Fallback title", "C++20", "Core Language",
            "Fallback summary", "", "int fallback = 42;"))
        compose.onNodeWithTag("detail_scroll_column").performScrollToNode(hasTestTag("code_block_text"))
        compose.onNodeWithTag("code_block_text").assertTextEquals("int fallback = 42;")
    }

    private fun showTip(tip: CppTip) {
        compose.setContent {
            MaterialTheme {
                DetailScreen(
                    tip = tip, allTips = listOf(tip), isLoading = false,
                    onBack = {}, onBookmarkToggle = {}, onDownloadTip = {},
                    onSaveNotes = { _, _ -> }, onNavigateToTip = {}
                )
            }
        }
    }
}
