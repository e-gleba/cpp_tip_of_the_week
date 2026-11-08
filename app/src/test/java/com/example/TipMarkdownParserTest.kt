package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.TipMarkdownParser
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TipMarkdownParserTest {
    @Test
    fun `policy design preserves all eight examples in source order`() {
        val document = bundledTip(209)
        val blocks = codeBlocks(document)
        assertEquals(8, blocks.size)
        assertTrue(blocks.first().literal.contains("struct foo : TPolicy"))
        assertTrue(blocks[1].literal.contains("return 0;/*TODO*/"))
        assertTrue(blocks.last().literal.contains("}() + ... + 0)"))
        assertEquals(listOf("Info", "Example", "Puzzle", "Solutions"), headings(document))
        assertNoSectionHtml(document)
    }

    @Test
    fun `stacktrace preserves shell output and all eleven source solutions with their links`() {
        val document = bundledTip(295)
        val blocks = codeBlocks(document)
        assertEquals(14, blocks.size)
        assertEquals(listOf("cpp", "sh", "cpp") + List(11) { "cpp" }, blocks.map { it.info })
        assertTrue(blocks[1].literal.contains("0# foo()"))
        assertTrue(blocks[3].literal.contains("return 1;"))
        assertTrue(blocks.last().literal.contains("return (entry-s.begin());"))
        val links = mutableListOf<String>()
        document.accept(object : AbstractVisitor() {
            override fun visit(node: Text) {
                Regex("""https://godbolt\.org/z/[A-Za-z0-9]+""").findAll(node.literal)
                    .forEach { links.add(it.value.substringAfterLast('/')) }
            }
        })
        assertEquals(listOf("dd9x7Mejc", "rhE8EKf73", "jsdPaP6Px", "hzj3nd36o",
            "EdTT115es", "aG1n9j4ba", "v9nY35PK5", "hx9KbPTvr", "MfPGMc4xv",
            "9x84cfY4h", "nfKda9oGr", "G61dhrYsr", "oTWz4h6z4"), links)
        assertNoSectionHtml(document)
    }

    @Test
    fun `multiple solution blocks are not truncated or merged`() {
        assertEquals(4, codeBlocks(bundledTip(182)).size)
        assertEquals(8, codeBlocks(bundledTip(200)).size)
    }

    @Test
    fun `longer fences preserve triple backticks and HTML inside code`() {
        val source = "````cpp\nconst auto html = \"<details><summary>literal</summary></details>\";\n```\nint value = 42;\n````\n"
        val blocks = codeBlocks(TipMarkdownParser.parse(source))
        assertEquals(1, blocks.size)
        assertEquals("const auto html = \"<details><summary>literal</summary></details>\";\n```\nint value = 42;\n", blocks.single().literal)
    }

    @Test
    fun `code nested in list items remains distinct and ordered`() {
        val source = "- First\n\n  ```cpp\n  int first = 1;\n  ```\n\n- Second\n\n  ~~~cpp\n  int second = 2;\n  ~~~\n"
        assertEquals(listOf("int first = 1;\n", "int second = 2;\n"),
            codeBlocks(TipMarkdownParser.parse(source)).map { it.literal })
    }

    private fun bundledTip(id: Int): Node {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val markdown = context.assets.open("tips/$id.md").bufferedReader().use { it.readText() }
        return TipMarkdownParser.parse(markdown)
    }

    private fun codeBlocks(document: Node): List<FencedCodeBlock> {
        val blocks = mutableListOf<FencedCodeBlock>()
        document.accept(object : AbstractVisitor() {
            override fun visit(node: FencedCodeBlock) { blocks.add(node) }
        })
        return blocks
    }

    private fun headings(document: Node): List<String> {
        val headings = mutableListOf<String>()
        document.accept(object : AbstractVisitor() {
            override fun visit(node: Heading) {
                headings.add((node.firstChild as Text).literal)
            }
        })
        return headings
    }

    private fun assertNoSectionHtml(document: Node) {
        document.accept(object : AbstractVisitor() {
            override fun visit(node: HtmlBlock) {
                assertFalse(node.literal.contains("<details"))
                assertFalse(node.literal.contains("<summary"))
            }
        })
    }
}
