package com.nounish.launcher.model

import android.os.UserHandle
import java.text.Normalizer
import java.util.Locale

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String? = null,
    val customLabel: String? = null,
    val userHandle: UserHandle? = null,
    val isWorkProfile: Boolean = false
) {
    /**
     * Unique key distinguishing:
     * - Different packages
     * - Multiple launcher activities within the same package
     * - Work profile vs personal profile
     */
    val key: String
        get() = buildString {
            append(packageName)
            if (!activityName.isNullOrBlank()) {
                append("/")
                append(activityName)
            }
            if (isWorkProfile) {
                append("#work")
            }
        }

    val displayLabel: String
        get() {
            val base = customLabel?.takeIf { it.isNotBlank() } ?: label
            return if (isWorkProfile) "$base (Work)" else base
        }

    companion object {
        fun normalizeForSorting(text: String): String {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return ""
            return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replace("\\p{M}+".toRegex(), "")
        }

        fun getAlphabetBucket(text: String): Char {
            val clean = normalizeForSorting(text)
            val first = clean.firstOrNull()?.uppercaseChar() ?: return '#'
            return if (first in 'A'..'Z') first else '#'
        }

        /**
         * Comparator sorting:
         * 1. Letters A–Z (case- and accent-insensitively)
         * 2. Names starting with digits or symbols under '#' at the end
         */
        val COMPARATOR = Comparator<AppItem> { a, b ->
            val nameA = a.displayLabel
            val nameB = b.displayLabel

            val bucketA = getAlphabetBucket(nameA)
            val bucketB = getAlphabetBucket(nameB)

            val isLetterA = bucketA in 'A'..'Z'
            val isLetterB = bucketB in 'A'..'Z'

            when {
                isLetterA && !isLetterB -> -1
                !isLetterA && isLetterB -> 1
                else -> {
                    val normA = normalizeForSorting(nameA).lowercase(Locale.ROOT)
                    val normB = normalizeForSorting(nameB).lowercase(Locale.ROOT)
                    val cmp = normA.compareTo(normB)
                    if (cmp != 0) cmp else nameA.compareTo(nameB, ignoreCase = true)
                }
            }
        }
    }
}
