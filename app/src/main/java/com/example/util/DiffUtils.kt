package com.example.util

import java.util.regex.Pattern

enum class DiffType {
    ADDED,
    DELETED,
    UNCHANGED
}

data class DiffLine(
    val type: DiffType,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val content: String
)

data class SideBySideRow(
    val leftLineNumber: Int?,
    val leftContent: String?,
    val leftType: DiffType?,
    val rightLineNumber: Int?,
    val rightContent: String?,
    val rightType: DiffType?
)

data class DiffResult(
    val lines: List<DiffLine>,
    val additions: Int,
    val deletions: Int,
    val unchanged: Int,
    val hasChanges: Boolean
)

object DiffUtils {

    /**
     * Normalizes HTML string to be cleanly diffable line-by-line.
     */
    fun normalizeHtml(rawHtml: String, stripScripts: Boolean = true, formatTags: Boolean = true): String {
        var html = rawHtml

        if (stripScripts) {
            // Remove script tags and contents
            val scriptPattern = Pattern.compile("(?is)<script.*?</script>")
            html = scriptPattern.matcher(html).replaceAll("")
            
            // Remove style tags and contents if desired
            val stylePattern = Pattern.compile("(?is)<style.*?</style>")
            html = stylePattern.matcher(html).replaceAll("")

            // Remove common volatile comments & tracker tokens
            val commentPattern = Pattern.compile("(?is)<!--.*?-->")
            html = commentPattern.matcher(html).replaceAll("")
        }

        if (formatTags) {
            // Insert newlines between tags to break single-line minified HTML into distinct readable lines
            html = html.replace("><", ">\n<")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
        }

        val lines = html.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }

        return lines.joinToString("\n")
    }

    /**
     * Computes line-by-line Myers / LCS difference between two texts.
     */
    fun computeDiff(oldText: String?, newText: String?): DiffResult {
        val oldLines = if (oldText.isNullOrEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isNullOrEmpty()) emptyList() else newText.lines()

        if (oldLines.isEmpty() && newLines.isEmpty()) {
            return DiffResult(emptyList(), 0, 0, 0, false)
        }

        if (oldLines.isEmpty()) {
            val lines = newLines.mapIndexed { idx, line ->
                DiffLine(DiffType.ADDED, null, idx + 1, line)
            }
            return DiffResult(lines, lines.size, 0, 0, true)
        }

        if (newLines.isEmpty()) {
            val lines = oldLines.mapIndexed { idx, line ->
                DiffLine(DiffType.DELETED, idx + 1, null, line)
            }
            return DiffResult(lines, 0, lines.size, 0, true)
        }

        // Longest Common Subsequence (LCS) matrix
        val n = oldLines.size
        val m = newLines.size

        // If files are very large (> 2500 lines), trim unchanged prefix and suffix for performance
        var prefixLen = 0
        while (prefixLen < n && prefixLen < m && oldLines[prefixLen] == newLines[prefixLen]) {
            prefixLen++
        }

        var suffixLen = 0
        while (suffixLen < (n - prefixLen) && suffixLen < (m - prefixLen) &&
            oldLines[n - 1 - suffixLen] == newLines[m - 1 - suffixLen]) {
            suffixLen++
        }

        val midOld = oldLines.subList(prefixLen, n - suffixLen)
        val midNew = newLines.subList(prefixLen, m - suffixLen)

        val midOldSize = midOld.size
        val midNewSize = midNew.size

        val dp = Array(midOldSize + 1) { IntArray(midNewSize + 1) }

        for (i in 1..midOldSize) {
            for (j in 1..midNewSize) {
                if (midOld[i - 1] == midNew[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val resultLines = mutableListOf<DiffLine>()

        // 1. Add common prefix
        for (i in 0 until prefixLen) {
            resultLines.add(DiffLine(DiffType.UNCHANGED, i + 1, i + 1, oldLines[i]))
        }

        // 2. Backtrack mid diff
        val midDiffList = mutableListOf<DiffLine>()
        var i = midOldSize
        var j = midNewSize

        var currentOldLine = prefixLen + midOldSize
        var currentNewLine = prefixLen + midNewSize

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && midOld[i - 1] == midNew[j - 1]) {
                midDiffList.add(
                    DiffLine(DiffType.UNCHANGED, currentOldLine, currentNewLine, midOld[i - 1])
                )
                i--
                j--
                currentOldLine--
                currentNewLine--
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                midDiffList.add(
                    DiffLine(DiffType.ADDED, null, currentNewLine, midNew[j - 1])
                )
                j--
                currentNewLine--
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                midDiffList.add(
                    DiffLine(DiffType.DELETED, currentOldLine, null, midOld[i - 1])
                )
                i--
                currentOldLine--
            }
        }

        midDiffList.reverse()
        resultLines.addAll(midDiffList)

        // 3. Add common suffix
        for (k in 0 until suffixLen) {
            val oldIdx = n - suffixLen + k
            val newIdx = m - suffixLen + k
            resultLines.add(DiffLine(DiffType.UNCHANGED, oldIdx + 1, newIdx + 1, oldLines[oldIdx]))
        }

        var additions = 0
        var deletions = 0
        var unchanged = 0

        for (line in resultLines) {
            when (line.type) {
                DiffType.ADDED -> additions++
                DiffType.DELETED -> deletions++
                DiffType.UNCHANGED -> unchanged++
            }
        }

        return DiffResult(
            lines = resultLines,
            additions = additions,
            deletions = deletions,
            unchanged = unchanged,
            hasChanges = additions > 0 || deletions > 0
        )
    }

    /**
     * Filters diff lines to only show changed lines and surrounding context lines (e.g. 3 lines of context).
     */
    fun filterHunksWithContext(lines: List<DiffLine>, contextLines: Int = 3): List<DiffLine> {
        if (lines.isEmpty()) return emptyList()

        val changedIndices = mutableSetOf<Int>()
        lines.forEachIndexed { index, line ->
            if (line.type != DiffType.UNCHANGED) {
                for (c in (index - contextLines).coerceAtLeast(0)..(index + contextLines).coerceAtMost(lines.lastIndex)) {
                    changedIndices.add(c)
                }
            }
        }

        if (changedIndices.isEmpty()) {
            // No changes, take first 10 lines as sample
            return lines.take(10)
        }

        return lines.filterIndexed { index, _ -> changedIndices.contains(index) }
    }

    /**
     * Converts diff lines into structured side-by-side comparison rows (Left = Before, Right = After).
     */
    fun computeSideBySideRows(lines: List<DiffLine>): List<SideBySideRow> {
        val result = mutableListOf<SideBySideRow>()
        var idx = 0

        while (idx < lines.size) {
            val line = lines[idx]
            when (line.type) {
                DiffType.UNCHANGED -> {
                    result.add(
                        SideBySideRow(
                            leftLineNumber = line.oldLineNumber,
                            leftContent = line.content,
                            leftType = DiffType.UNCHANGED,
                            rightLineNumber = line.newLineNumber,
                            rightContent = line.content,
                            rightType = DiffType.UNCHANGED
                        )
                    )
                    idx++
                }
                DiffType.DELETED -> {
                    // Collect consecutive deletions
                    val deletions = mutableListOf<DiffLine>()
                    while (idx < lines.size && lines[idx].type == DiffType.DELETED) {
                        deletions.add(lines[idx])
                        idx++
                    }

                    // Collect subsequent consecutive additions
                    val additions = mutableListOf<DiffLine>()
                    while (idx < lines.size && lines[idx].type == DiffType.ADDED) {
                        additions.add(lines[idx])
                        idx++
                    }

                    val maxCount = maxOf(deletions.size, additions.size)
                    for (k in 0 until maxCount) {
                        val del = deletions.getOrNull(k)
                        val add = additions.getOrNull(k)
                        result.add(
                            SideBySideRow(
                                leftLineNumber = del?.oldLineNumber,
                                leftContent = del?.content,
                                leftType = if (del != null) DiffType.DELETED else null,
                                rightLineNumber = add?.newLineNumber,
                                rightContent = add?.content,
                                rightType = if (add != null) DiffType.ADDED else null
                            )
                        )
                    }
                }
                DiffType.ADDED -> {
                    // Unpaired addition
                    result.add(
                        SideBySideRow(
                            leftLineNumber = null,
                            leftContent = null,
                            leftType = null,
                            rightLineNumber = line.newLineNumber,
                            rightContent = line.content,
                            rightType = DiffType.ADDED
                        )
                    )
                    idx++
                }
            }
        }
        return result
    }
}
