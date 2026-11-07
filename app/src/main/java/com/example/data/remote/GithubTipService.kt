package com.example.data.remote

import com.example.data.asset.AssetTipLoader
import com.example.data.model.CppTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

class RateLimitException(message: String) : IOException(message)

class GithubTipService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Downloads the whole GitHub repository zip archive in ONE single request (~200KB)
     * and unpacks all tips in memory with zero API rate limits or batch delays.
     */
    suspend fun syncFromRepoArchive(): Result<List<CppTip>> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://github.com/tip-of-the-week/cpp/archive/refs/heads/main.zip",
            "https://github.com/tip-of-the-week/cpp/archive/refs/heads/master.zip"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CppTotW-Android-App")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val stream = response.body?.byteStream() ?: continue
                    val tips = mutableListOf<CppTip>()
                    val zipStream = ZipInputStream(stream)
                    var entry = zipStream.nextEntry
                    val tipPattern = Pattern.compile("tips/(\\d+)\\.md$")

                    while (entry != null) {
                        val name = entry.name
                        val matcher = tipPattern.matcher(name)
                        if (matcher.find()) {
                            val id = matcher.group(1)?.toIntOrNull()
                            if (id != null) {
                                val buffer = ByteArrayOutputStream()
                                val temp = ByteArray(4096)
                                var len: Int
                                while (zipStream.read(temp).also { len = it } > 0) {
                                    buffer.write(temp, 0, len)
                                }
                                val markdown = buffer.toString(Charsets.UTF_8.name())
                                if (markdown.isNotBlank()) {
                                    val tip = AssetTipLoader.parseMarkdownToTip(id, markdown)
                                    tips.add(tip)
                                }
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                    zipStream.close()

                    if (tips.isNotEmpty()) {
                        return@withContext Result.success(tips.sortedBy { it.id })
                    }
                }
            } catch (_: Exception) {
                // try fallback
            }
        }
        Result.failure(IOException("Could not sync repository archive from GitHub."))
    }

    /**
     * Fetches the tips catalog first from GitHub Pages (tip-of-the-week.github.io/cpp)
     * which has NO API limits, then falls back to raw CDN README.
     */
    suspend fun fetchCatalogFromWeb(): Result<List<CppTip>> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://tip-of-the-week.github.io/cpp/",
            "https://raw.githubusercontent.com/tip-of-the-week/cpp/master/README.md",
            "https://raw.githubusercontent.com/tip-of-the-week/cpp/main/README.md"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CppTotW-Android-App")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val parsed = if (url.contains("github.io")) {
                            parseHtmlOrMarkdownIndex(body)
                        } else {
                            parseReadmeToTips(body)
                        }

                        if (parsed.isNotEmpty()) {
                            return@withContext Result.success(parsed)
                        }
                    }
                }
            } catch (_: Exception) {
                // try fallback
            }
        }
        Result.failure(IOException("Could not fetch tips catalog from tip-of-the-week.github.io/cpp/"))
    }

    suspend fun fetchTipFromWeb(tipId: Int): Result<CppTip> = withContext(Dispatchers.IO) {
        // Fetch raw markdown without hitting REST API
        val urls = listOf(
            "https://raw.githubusercontent.com/tip-of-the-week/cpp/master/tips/$tipId.md",
            "https://raw.githubusercontent.com/tip-of-the-week/cpp/main/tips/$tipId.md"
        )

        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CppTotW-Android-App")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.code == 429) {
                    return@withContext Result.failure(
                        RateLimitException("GitHub rate limit reached (HTTP 429). Pausing sync.")
                    )
                }
                if (response.isSuccessful) {
                    val markdown = response.body?.string() ?: ""
                    if (markdown.isNotBlank()) {
                        val tip = parseMarkdownToTip(tipId, markdown)
                        return@withContext Result.success(tip)
                    }
                }
            } catch (e: RateLimitException) {
                return@withContext Result.failure(e)
            } catch (_: Exception) {
                // try fallback
            }
        }
        Result.failure(IOException("Tip #$tipId not available."))
    }

    private fun parseHtmlOrMarkdownIndex(content: String): List<CppTip> {
        val tips = mutableListOf<CppTip>()

        // 1. Try HTML link pattern: <a href="...tips/182...">Title</a>
        val htmlPattern = Pattern.compile(
            "<a[^>]*href=[\"'][^\"']*(?:tips/)?(\\d+)(?:\\.html|\\.md)?[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE
        )
        val htmlMatcher = htmlPattern.matcher(content)
        while (htmlMatcher.find()) {
            val idStr = htmlMatcher.group(1) ?: continue
            val id = idStr.toIntOrNull() ?: continue
            val rawTitle = htmlMatcher.group(2)?.replace(Regex("<.*?>"), "")?.trim() ?: "Tip #$id"
            if (rawTitle.isNotBlank() && !tips.any { it.id == id }) {
                tips.add(createCatalogEntry(id, rawTitle))
            }
        }

        // 2. If HTML didn't match enough, try markdown links
        if (tips.size < 10) {
            tips.addAll(parseReadmeToTips(content))
        }

        return tips.distinctBy { it.id }
    }

    private fun parseReadmeToTips(readmeMarkdown: String): List<CppTip> {
        val tips = mutableListOf<CppTip>()
        val lines = readmeMarkdown.lines()
        var currentStandard = "C++20"

        val linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(tips/(\\d+)\\.md\\)", Pattern.CASE_INSENSITIVE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("##") || trimmed.startsWith("###")) {
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

            val matcher = linkPattern.matcher(line)
            while (matcher.find()) {
                val rawTitle = matcher.group(1)?.trim() ?: ""
                val idStr = matcher.group(2) ?: ""
                val id = idStr.toIntOrNull() ?: continue

                val cleanTitle = rawTitle
                    .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
                    .replace("*", "")
                    .replace("`", "")
                    .trim()

                tips.add(createCatalogEntry(id, cleanTitle, currentStandard))
            }
        }
        return tips
    }

    private fun createCatalogEntry(id: Int, title: String, fallbackStandard: String = "C++20"): CppTip {
        val standard = when {
            title.contains("C++26", ignoreCase = true) -> "C++26"
            title.contains("C++23", ignoreCase = true) -> "C++23"
            title.contains("C++20", ignoreCase = true) -> "C++20"
            title.contains("C++17", ignoreCase = true) -> "C++17"
            title.contains("C++14", ignoreCase = true) -> "C++14"
            else -> fallbackStandard
        }

        return CppTip(
            id = id,
            title = title,
            standard = standard,
            category = inferCategory(title),
            summary = "Tip #$id from tip-of-the-week.github.io/cpp/. Tap to view complete code.",
            content = "Tip #$id indexed from tip-of-the-week.github.io/cpp/.",
            codeExample = "// Tip #$id from tip-of-the-week.github.io/cpp/\n// Loading full code...",
            puzzleSnippet = null,
            puzzleAnswer = null,
            godboltUrl = null,
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/main/tips/$id.md",
            isBookmarked = false,
            isFeatured = false,
            isDownloaded = false
        )
    }

    private fun parseMarkdownToTip(id: Int, markdown: String): CppTip {
        val lines = markdown.lines()

        var title = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                title = trimmed.replace(Regex("^#+\\s*"), "").trim()
                break
            } else if (trimmed.startsWith("Did you know", ignoreCase = true) ||
                trimmed.startsWith("Tip", ignoreCase = true)
            ) {
                title = trimmed
                break
            }
        }
        if (title.isBlank()) {
            title = "Tip #$id"
        }

        title = title
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace("*", "")
            .replace("`", "")
            .trim()

        val standard = when {
            markdown.contains("C++26", ignoreCase = true) -> "C++26"
            markdown.contains("C++23", ignoreCase = true) -> "C++23"
            markdown.contains("C++20", ignoreCase = true) -> "C++20"
            markdown.contains("C++17", ignoreCase = true) -> "C++17"
            markdown.contains("C++14", ignoreCase = true) -> "C++14"
            else -> "C++11"
        }

        val category = inferCategory(markdown)

        val codeBlocks = mutableListOf<String>()
        val codeBlockPattern = Pattern.compile("```(?:cpp|c\\+\\+)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
        val matcher = codeBlockPattern.matcher(markdown)
        while (matcher.find()) {
            val code = matcher.group(1)?.trim()
            if (!code.isNullOrBlank()) {
                codeBlocks.add(code)
            }
        }

        val primaryCode = codeBlocks.firstOrNull() ?: "// Tip #$id from tip-of-the-week.github.io/cpp/\n// See explanation below."
        val puzzleSnippet = if (codeBlocks.size > 1) codeBlocks[1] else null

        val godboltPattern = Pattern.compile("https?://(?:www\\.)?godbolt\\.org/z/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE)
        val godboltMatcher = godboltPattern.matcher(markdown)
        val godboltUrl = if (godboltMatcher.find()) godboltMatcher.group(0) else null

        val textContent = markdown
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("^#+.*", RegexOption.MULTILINE), "")
            .trim()

        val summary = if (textContent.length > 180) {
            textContent.take(177).trim() + "..."
        } else if (textContent.isNotBlank()) {
            textContent
        } else {
            "C++ Tip #$id from tip-of-the-week.github.io/cpp/."
        }

        return CppTip(
            id = id,
            title = title,
            standard = standard,
            category = category,
            summary = summary,
            content = markdown,
            codeExample = primaryCode,
            puzzleSnippet = puzzleSnippet,
            puzzleAnswer = if (puzzleSnippet != null) "Check compiler assertions and Godbolt link for verification." else null,
            godboltUrl = godboltUrl,
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/main/tips/$id.md",
            isBookmarked = false,
            isFeatured = false,
            isDownloaded = true
        )
    }

    private fun inferCategory(text: String): String {
        return when {
            text.contains("concept", ignoreCase = true) || text.contains("template", ignoreCase = true) || text.contains("reflection", ignoreCase = true) || text.contains("pack", ignoreCase = true) -> "Templates & Metaprogramming"
            text.contains("thread", ignoreCase = true) || text.contains("atomic", ignoreCase = true) || text.contains("mutex", ignoreCase = true) || text.contains("async", ignoreCase = true) || text.contains("coroutine", ignoreCase = true) -> "Concurrency"
            text.contains("performance", ignoreCase = true) || text.contains("memory", ignoreCase = true) || text.contains("alloc", ignoreCase = true) || text.contains("elision", ignoreCase = true) || text.contains("cache", ignoreCase = true) -> "Performance & Memory"
            text.contains("std::", ignoreCase = true) || text.contains("views::", ignoreCase = true) || text.contains("<", ignoreCase = true) -> "Standard Library"
            else -> "Core Language"
        }
    }
}
