package com.example

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.CppSyntaxCodeBlock
import com.example.ui.components.TipMarkdownBlock
import com.example.ui.components.TipMarkdownParser
import com.example.ui.components.markdownChildren
import com.example.ui.components.markdownInline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarkdownComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `code stays complete and copyable without a duplicate title`() {
        val code = "  int value = 42;\n  // keep spaces\n"
        compose.setContent {
            MaterialTheme { CppSyntaxCodeBlock(code, title = "Tip #295 Code") }
        }
        compose.onNodeWithTag("code_block_text").assertTextEquals(code)
        compose.onNodeWithText("Tip #295 Code").assertDoesNotExist()
        compose.onNodeWithContentDescription("Copy code").performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        compose.runOnIdle {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(code, clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
    }

    @Test
    fun `renderer keeps separate code and output blocks without synthetic answers`() {
        val source = "## Example\n\n```cpp\nint value = 42;\n```\n\n```sh\nvalue: 42\n```\n\n> https://godbolt.org/z/example\n"
        val blocks = TipMarkdownParser.parse(source).markdownChildren()
        compose.setContent {
            MaterialTheme {
                Column { blocks.forEach { TipMarkdownBlock(it) } }
            }
        }
        compose.onAllNodesWithTag("code_block").assertCountEquals(2)
        compose.onNodeWithText("value: 42\n").assertExists()
        compose.onNodeWithText("https://godbolt.org/z/example").assertExists()
        compose.onNodeWithText("Check Godbolt compiler output to solve the puzzle.").assertDoesNotExist()
    }

    @Test
    fun `inline formatting preserves code and distinct link destinations`() {
        val document = TipMarkdownParser.parse("**Bold** and `std::vector<int>` [first](https://example.com/a) https://example.com/b")
        val formatted = markdownInline(document.firstChild, Color.Blue, Color.LightGray)
        assertTrue(formatted.text.contains("std::vector<int>"))
        assertFalse(formatted.text.contains("**"))
        assertEquals(listOf("https://example.com/a", "https://example.com/b"),
            formatted.getLinkAnnotations(0, formatted.length).map { (it.item as androidx.compose.ui.text.LinkAnnotation.Url).url })
    }
}
