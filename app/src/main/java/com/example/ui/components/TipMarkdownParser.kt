package com.example.ui.components

import org.commonmark.node.HtmlBlock
import org.commonmark.node.Node
import org.commonmark.parser.Parser

internal object TipMarkdownParser {
    private val parser = Parser.builder().build()
    private val summary = Regex("""<summary\b[^>]*>(.*?)</summary>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val wrappers = Regex("""</?(?:details|p)(?:\s[^>]*)?>""", RegexOption.IGNORE_CASE)

    fun parse(markdown: String): Node {
        val document = parser.parse(markdown)
        unwrapSections(document)
        return document
    }

    private fun unwrapSections(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            if (node is HtmlBlock &&
                (summary.containsMatchIn(node.literal) || wrappers.containsMatchIn(node.literal))) {
                // The upstream tips wrap Markdown in HTML details/summary sections.
                // Transform only parsed HTML nodes, never source text inside code fences.
                val markdown = summary.replace(node.literal) { "\n\n## ${it.groupValues[1]}\n\n" }
                    .replace(wrappers, "").trim()
                val replacement = parser.parse(markdown)
                var child = replacement.firstChild
                while (child != null) {
                    val following = child.next
                    node.insertBefore(child)
                    child = following
                }
                node.unlink()
            } else {
                unwrapSections(node)
            }
            node = next
        }
    }
}

internal fun Node.markdownChildren(): List<Node> {
    val children = mutableListOf<Node>()
    var child = firstChild
    while (child != null) {
        children.add(child)
        child = child.next
    }
    return children
}
