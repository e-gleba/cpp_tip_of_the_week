package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CodeBackground = Color(0xFF0D131F)
private val KeywordColor = Color(0xFF38BDF8)
private val TypeColor = Color(0xFFFACC15)
private val PreprocessorColor = Color(0xFFC084FC)
private val StringColor = Color(0xFFFB923C)
private val CommentColor = Color(0xFF4ADE80)

@Composable
fun CppSyntaxCodeBlock(
    code: String,
    title: String = "Code",
    modifier: Modifier = Modifier,
    highlight: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopied by remember(code) { mutableStateOf(false) }
    val formatted = remember(code, highlight) {
        if (highlight) buildAnnotatedString {
            code.lines().forEachIndexed { index, line ->
                if (index > 0) append('\n')
                append(highlightCppSyntax(line))
            }
        } else AnnotatedString(code)
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("code_block"),
        shape = RoundedCornerShape(10.dp),
        color = CodeBackground
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(title, code))
                    isCopied = true
                    scope.launch {
                        delay(2000)
                        isCopied = false
                    }
                }) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) CommentColor else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = formatted,
                    modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                        .testTag("code_block_text"),
                    softWrap = false,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    }
}

fun highlightCppSyntax(line: String): AnnotatedString {
    return buildAnnotatedString {
        val trimmed = line.trimStart()

        // Handle full line comments
        if (trimmed.startsWith("//") || trimmed.startsWith("/*")) {
            append(line)
            addStyle(
                SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic),
                0,
                line.length
            )
            return@buildAnnotatedString
        }

        // Handle preprocessor directives
        if (trimmed.startsWith("#")) {
            append(line)
            val directiveEnd = line.indexOf(' ', line.indexOf('#')).let { if (it == -1) line.length else it }
            addStyle(SpanStyle(color = PreprocessorColor, fontWeight = FontWeight.Bold), line.indexOf('#'), directiveEnd)
            if (directiveEnd < line.length) {
                addStyle(SpanStyle(color = StringColor), directiveEnd, line.length)
            }
            return@buildAnnotatedString
        }

        append(line)

        // Check for inline comments
        val commentIndex = line.indexOf("//")
        val effectiveLength = if (commentIndex != -1) commentIndex else line.length
        if (commentIndex != -1) {
            addStyle(SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic), commentIndex, line.length)
        }

        // Token regexes
        val keywords = setOf(
            "template", "typename", "concept", "requires", "constexpr", "consteval", "constinit",
            "auto", "struct", "class", "enum", "using", "namespace", "static_assert", "noexcept",
            "inline", "virtual", "override", "final", "public", "private", "protected", "return",
            "if", "else", "for", "while", "switch", "case", "break", "continue", "default",
            "sizeof", "new", "delete", "try", "catch", "throw", "explicit", "friend", "decltype",
            "nullptr", "this", "static", "const", "volatile", "mutable"
        )

        val types = setOf(
            "int", "char", "bool", "double", "float", "void", "size_t", "uint32_t", "int64_t",
            "string", "string_view", "vector", "span", "expected", "unexpected", "optional",
            "tuple", "array", "unique_ptr", "shared_ptr", "jthread", "thread", "stop_token"
        )

        // Highlight strings
        val stringRegex = Regex("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'")
        for (match in stringRegex.findAll(line.take(effectiveLength))) {
            addStyle(SpanStyle(color = StringColor), match.range.first, match.range.last + 1)
        }

        // Highlight words
        val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        for (match in wordRegex.findAll(line.take(effectiveLength))) {
            val word = match.value
            when {
                word in keywords -> {
                    addStyle(
                        SpanStyle(color = KeywordColor, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
                word in types || word.startsWith("std::") -> {
                    addStyle(
                        SpanStyle(color = TypeColor),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
        }
    }
}
