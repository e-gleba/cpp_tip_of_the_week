package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cpp_tips")
data class CppTip(
    @PrimaryKey
    val id: Int,
    val title: String,
    val standard: String, // e.g. "C++11", "C++14", "C++17", "C++20", "C++23", "C++26"
    val category: String, // e.g. "Modern Idioms", "Standard Library", "Templates & Metaprogramming", "Core Language", "Performance & Memory"
    val summary: String,
    val content: String,
    val codeExample: String,
    val puzzleSnippet: String? = null,
    val puzzleAnswer: String? = null,
    val godboltUrl: String? = null,
    val githubUrl: String = "https://github.com/tip-of-the-week/cpp/blob/main/tips/$id.md",
    val isBookmarked: Boolean = false,
    val userNotes: String? = null,
    val isFeatured: Boolean = false,
    val isDownloaded: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
