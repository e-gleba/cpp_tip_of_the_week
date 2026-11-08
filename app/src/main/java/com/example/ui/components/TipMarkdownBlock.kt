package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Text as MarkdownText

@Composable
internal fun TipMarkdownBlock(node: Node, modifier: Modifier = Modifier) {
    when (node) {
        is FencedCodeBlock -> CppSyntaxCodeBlock(
            code = node.literal, modifier = modifier,
            highlight = node.info.substringBefore(' ').lowercase() in setOf("cpp", "c++", "cxx", "cc", "c")
        )
        is IndentedCodeBlock -> CppSyntaxCodeBlock(code = node.literal, modifier = modifier, highlight = false)
        is Heading -> MarkdownTextBlock(node, modifier, heading = node.level)
        is Paragraph -> MarkdownTextBlock(node, modifier)
        is BulletList, is OrderedList -> {
            val children = remember(node) { node.markdownChildren() }
            val start = (node as? OrderedList)?.markerStartNumber ?: 1
            Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                children.forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (node is OrderedList) "${start + index}." else "•")
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item.markdownChildren().forEach { TipMarkdownBlock(it) }
                        }
                    }
                }
            }
        }
        is BlockQuote -> Column(modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.markdownChildren().forEach { TipMarkdownBlock(it) }
        }
        is ThematicBreak -> HorizontalDivider(modifier)
        is HtmlBlock -> Text(AnnotatedString.fromHtml(node.literal), modifier = modifier)
        else -> Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.markdownChildren().forEach { TipMarkdownBlock(it) }
        }
    }
}

@Composable
private fun MarkdownTextBlock(node: Node, modifier: Modifier, heading: Int = 0) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val content = remember(node, linkColor, codeBackground) { markdownInline(node, linkColor, codeBackground) }
    val style = when (heading) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        4, 5, 6 -> MaterialTheme.typography.titleSmall
        else -> MaterialTheme.typography.bodyMedium
    }
    SelectionContainer {
        Text(content, modifier.fillMaxWidth(), style = style, color = MaterialTheme.colorScheme.onSurface)
    }
}

private val bareUrl = Regex("""https?://[^\s<>]+""")

internal fun markdownInline(node: Node, linkColor: Color, codeBackground: Color): AnnotatedString =
    buildAnnotatedString { appendMarkdownNode(node, linkColor, codeBackground, true) }

private fun AnnotatedString.Builder.appendMarkdownNode(
    node: Node, linkColor: Color, codeBackground: Color, linkify: Boolean
) {
    fun children(links: Boolean = linkify) {
        node.markdownChildren().forEach { appendMarkdownNode(it, linkColor, codeBackground, links) }
    }
    val linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    when (node) {
        is MarkdownText -> {
            if (!linkify) {
                append(node.literal)
            } else {
                var position = 0
                bareUrl.findAll(node.literal).forEach { match ->
                    append(node.literal.substring(position, match.range.first))
                    withLink(LinkAnnotation.Url(match.value, linkStyles)) { append(match.value) }
                    position = match.range.last + 1
                }
                append(node.literal.substring(position))
            }
        }
        is Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
            append(node.literal)
        }
        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { children() }
        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { children() }
        is Link -> {
            if (node.destination.startsWith("https://", true) || node.destination.startsWith("http://", true)) {
                withLink(LinkAnnotation.Url(node.destination, linkStyles)) { children(false) }
            } else children(false)
        }
        is Image -> children(false)
        is SoftLineBreak -> append(' ')
        is HardLineBreak -> append('\n')
        is HtmlInline -> if (node.literal.lowercase() in setOf("<br>", "<br/>", "<br />")) append('\n')
        else -> children()
    }
}
