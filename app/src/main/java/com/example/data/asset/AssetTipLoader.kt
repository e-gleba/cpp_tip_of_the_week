package com.example.data.asset

import android.content.Context
import com.example.data.model.CppTip
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

object AssetTipLoader {
    /**
     * Loads bundled tips and catalog entries; missing files remain eligible for sync.
     */
    fun loadAllTipsFromAssets(context: Context): List<CppTip> {
        val assetManager = context.assets
        val standardMap = mutableMapOf<Int, String>()
        val titleMap = mutableMapOf<Int, String>()

        // 1. Read README.md from assets to extract categorized standards and titles
        try {
            assetManager.open("README.md").use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var currentStandard = "C++20"
                    val linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(https?://[^)]*tips/(\\d+)\\.md\\)", Pattern.CASE_INSENSITIVE)
                    val relLinkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(tips/(\\d+)\\.md\\)", Pattern.CASE_INSENSITIVE)

                    reader.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("###") || trimmed.startsWith("##")) {
                            val upper = trimmed.uppercase()
                            when {
                                upper.contains("C++26") -> currentStandard = "C++26"
                                upper.contains("C++23") -> currentStandard = "C++23"
                                upper.contains("C++20") -> currentStandard = "C++20"
                                upper.contains("C++17") -> currentStandard = "C++17"
                                upper.contains("C++14") -> currentStandard = "C++14"
                                upper.contains("C++11") -> currentStandard = "C++11"
                            }
                        }

                        var matcher = linkPattern.matcher(line)
                        while (matcher.find()) {
                            val rawTitle = matcher.group(1)?.trim() ?: ""
                            val id = matcher.group(2)?.toIntOrNull() ?: continue
                            val cleanTitle = cleanMarkdownTitle(rawTitle)
                            standardMap[id] = currentStandard
                            titleMap[id] = cleanTitle
                        }

                        matcher = relLinkPattern.matcher(line)
                        while (matcher.find()) {
                            val rawTitle = matcher.group(1)?.trim() ?: ""
                            val id = matcher.group(2)?.toIntOrNull() ?: continue
                            val cleanTitle = cleanMarkdownTitle(rawTitle)
                            standardMap[id] = currentStandard
                            titleMap[id] = cleanTitle
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // README fallback
        }

        // 2. Read each tips/*.md file from assets
        val tipFiles = try {
            assetManager.list("tips")?.filter { it.endsWith(".md") } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val tips = mutableListOf<CppTip>()
        for (filename in tipFiles) {
            val id = filename.removeSuffix(".md").toIntOrNull() ?: continue
            val markdown = try {
                assetManager.open("tips/$filename").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                continue
            }

            val fallbackStandard = standardMap[id] ?: "C++20"
            val fallbackTitle = titleMap[id]
            val parsedTip = parseMarkdownToTip(id, markdown, fallbackStandard, fallbackTitle)
            tips.add(parsedTip)
        }

        // 3. Fallback for tips listed in README if file was missing
        val loadedIds = tips.mapTo(mutableSetOf()) { it.id }
        for ((id, title) in titleMap) {
            if (id !in loadedIds) {
                tips.add(
                    CppTip(
                        id = id,
                        title = title,
                        standard = standardMap[id] ?: "C++20",
                        category = inferCategory(title),
                        summary = title,
                        content = "# Tip #$id\n\n$title\n\nRead more at https://github.com/tip-of-the-week/cpp/blob/main/tips/$id.md",
                        codeExample = "// Tip #$id: $title\n// Full code is available online or via Sync.",
                        puzzleSnippet = null,
                        puzzleAnswer = null,
                        godboltUrl = null,
                        githubUrl = "https://github.com/tip-of-the-week/cpp/blob/main/tips/$id.md",
                        isBookmarked = false,
                        isFeatured = false,
                        isDownloaded = false
                    )
                )
            }
        }

        return tips.sortedBy { it.id }
    }

    fun parseMarkdownToTip(
        id: Int,
        markdown: String,
        fallbackStandard: String = "C++20",
        fallbackTitle: String? = null
    ): CppTip {
        var title = fallbackTitle ?: ""

        // Extract "Did you know..." title pattern
        if (title.isBlank()) {
            val didYouKnowMatcher = Pattern.compile("\\*\\*\\s*(Did you know[^*]+)\\*\\*", Pattern.CASE_INSENSITIVE).matcher(markdown)
            if (didYouKnowMatcher.find()) {
                title = didYouKnowMatcher.group(1)?.trim() ?: ""
            }
        }

        // Extract "# ..." header title pattern
        if (title.isBlank()) {
            val headerMatcher = Pattern.compile("^#+\\s*(.+)", Pattern.MULTILINE).matcher(markdown)
            if (headerMatcher.find()) {
                title = headerMatcher.group(1)?.trim() ?: ""
            }
        }

        if (title.isBlank()) {
            title = "Tip #$id"
        }

        title = cleanMarkdownTitle(title)

        val standard = when {
            markdown.contains("C++26", ignoreCase = true) || title.contains("C++26", ignoreCase = true) -> "C++26"
            markdown.contains("C++23", ignoreCase = true) || title.contains("C++23", ignoreCase = true) -> "C++23"
            markdown.contains("C++20", ignoreCase = true) || title.contains("C++20", ignoreCase = true) -> "C++20"
            markdown.contains("C++17", ignoreCase = true) || title.contains("C++17", ignoreCase = true) -> "C++17"
            markdown.contains("C++14", ignoreCase = true) || title.contains("C++14", ignoreCase = true) -> "C++14"
            markdown.contains("C++11", ignoreCase = true) || title.contains("C++11", ignoreCase = true) -> "C++11"
            else -> fallbackStandard
        }

        val codeBlocks = mutableListOf<String>()
        val codeBlockPattern = Pattern.compile("```(?:cpp|c\\+\\+)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
        val matcher = codeBlockPattern.matcher(markdown)
        while (matcher.find()) {
            val code = matcher.group(1)?.trim()
            if (!code.isNullOrBlank()) {
                codeBlocks.add(code)
            }
        }

        val primaryCode = codeBlocks.firstOrNull() ?: "// Tip #$id\n// See details and godbolt link below."
        val puzzleSnippet = if (codeBlocks.size > 1) codeBlocks[1] else null

        val godboltPattern = Pattern.compile("https?://(?:www\\.)?godbolt\\.org/z/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE)
        val godboltMatcher = godboltPattern.matcher(markdown)
        val godboltUrl = if (godboltMatcher.find()) godboltMatcher.group(0) else null

        val textContent = markdown
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("^#+.*", RegexOption.MULTILINE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val summary = if (textContent.length > 200) {
            textContent.take(197).trim() + "..."
        } else if (textContent.isNotBlank()) {
            textContent
        } else {
            title
        }

        return CppTip(
            id = id,
            title = title,
            standard = standard,
            category = inferCategory("$title $markdown"),
            summary = summary,
            content = markdown,
            codeExample = primaryCode,
            puzzleSnippet = puzzleSnippet,
            puzzleAnswer = if (puzzleSnippet != null) "Check Godbolt compiler output to solve the puzzle." else null,
            godboltUrl = godboltUrl,
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/main/tips/$id.md",
            isBookmarked = false,
            isFeatured = false,
            isDownloaded = true
        )
    }

    private fun cleanMarkdownTitle(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace("*", "")
            .replace("`", "")
            .trim()
    }

    fun inferCategory(text: String): String {
        return when {
            text.contains("concept", ignoreCase = true) || text.contains("template", ignoreCase = true) || text.contains("reflection", ignoreCase = true) || text.contains("pack", ignoreCase = true) -> "Templates & Metaprogramming"
            text.contains("thread", ignoreCase = true) || text.contains("atomic", ignoreCase = true) || text.contains("mutex", ignoreCase = true) || text.contains("async", ignoreCase = true) || text.contains("coroutine", ignoreCase = true) -> "Concurrency"
            text.contains("performance", ignoreCase = true) || text.contains("memory", ignoreCase = true) || text.contains("alloc", ignoreCase = true) || text.contains("elision", ignoreCase = true) || text.contains("cache", ignoreCase = true) -> "Performance & Memory"
            text.contains("std::", ignoreCase = true) || text.contains("views::", ignoreCase = true) || text.contains("<", ignoreCase = true) -> "Standard Library"
            else -> "Core Language"
        }
    }
}
