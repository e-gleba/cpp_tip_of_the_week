package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CodeBackground = Color(0xFF0D131F)
private val CodeHeaderBg = Color(0xFF131B2E)
private val CodeBorder = Color(0xFF1E293B)
private val LineNumberColor = Color(0xFF475569)

private val KeywordColor = Color(0xFF38BDF8)
private val TypeColor = Color(0xFFFACC15)
private val PreprocessorColor = Color(0xFFC084FC)
private val StringColor = Color(0xFFFB923C)
private val CommentColor = Color(0xFF4ADE80)
private val PunctuationColor = Color(0xFF94A3B8)

@Composable
fun CppSyntaxCodeBlock(
    code: String,
    title: String = "C++ Code",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CodeBorder, RoundedCornerShape(12.dp)),
        color = CodeBackground
    ) {
        Column {
            // Header bar: Terminal dots, title, and Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeHeaderBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Terminal window dots
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("C++ Code", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        isCopied = true
                        scope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Code Content with Line Numbers
            val horizontalScrollState = rememberScrollState()
            val lines = code.lines()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Line Numbers Column
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 14.dp)
                ) {
                    lines.indices.forEach { index ->
                        Text(
                            text = (index + 1).toString().padStart(2, ' '),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = LineNumberColor
                            )
                        )
                    }
                }

                // Highlighted Code Lines
                Column(
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    lines.forEach { line ->
                        val annotatedLine = highlightCppSyntax(line)
                        Text(
                            text = annotatedLine,
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
