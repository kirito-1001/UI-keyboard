/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.nlp

import android.content.Context

/**
 * A word list with frequencies for one language, used for completing the word being typed.
 *
 * The list is read from `assets/ime/dict/<language>.txt` (one `word<TAB>count` per line, most frequent first). Words
 * are searched by a normalized key (for Arabic: without diacritics and with the different forms of alef, ya and
 * ta marbuta treated as the same letter), so that a prefix typed a bit differently still finds the word.
 */
class UiDictionary private constructor(
    private val keys: Array<String>,
    private val words: Array<String>,
    private val counts: IntArray,
    private val language: String,
) {
    val size: Int get() = words.size

    /** Returns up to [limit] words starting with [prefix], most frequent first (the typed word itself is skipped). */
    fun complete(prefix: String, limit: Int): List<String> {
        val key = normalize(prefix, language)
        if (key.isEmpty()) return emptyList()
        // First index whose key is >= the prefix (keys are sorted)
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < key) lo = mid + 1 else hi = mid
        }
        // Collect the best matches by count (small top-N insertion)
        val best = ArrayList<Int>(limit + 1)
        var i = lo
        while (i < keys.size && keys[i].startsWith(key)) {
            if (keys[i] != key || words[i] != prefix) {
                var pos = best.size
                while (pos > 0 && counts[best[pos - 1]] < counts[i]) pos--
                if (pos < limit) {
                    best.add(pos, i)
                    if (best.size > limit) best.removeAt(best.size - 1)
                }
            }
            i++
        }
        return best.map { words[it] }
    }

    /** True if [word] is in the list (compared by the normalized key, so diacritics and alef forms do not matter). */
    fun contains(word: String): Boolean = indexOfKey(normalize(word, language)) >= 0

    /** Index of the most frequent entry with exactly this normalized [key], or -1. */
    private fun indexOfKey(key: String): Int {
        if (key.isEmpty()) return -1
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < key) lo = mid + 1 else hi = mid
        }
        return if (lo < keys.size && keys[lo] == key) lo else -1
    }

    /**
     * Words of the list that are closest to the misspelled [typed], best first (the closest, then the most frequent).
     *
     * Closeness is an edit distance: a wrong, missing or extra letter, or two swapped letters, each count 1. A wrong
     * letter that sits next to the right one on the keyboard counts only 0.6, so it is the most likely typo.
     * Short words (up to 4 letters) may differ by one edit, longer words by two. Nothing is returned for a word shorter
     * than 3 letters. A word that is in the list is treated as correct, except when it is very rare: the list (made
     * from subtitles) also holds slang and typos, so then only a word that is at least 100 times more frequent
     * is suggested instead (for example "tha" -> "the").
     */
    fun corrections(typed: String, limit: Int): List<String> {
        val key = normalize(typed, language)
        if (key.length < 3 || key.length > 24) return emptyList()
        val own = indexOfKey(key)
        val ownCount = if (own >= 0) counts[own].toLong() else 0L
        val maxEdits = if (key.length <= 4) 1 else 2
        val maxCost = maxEdits.toDouble()
        val buffers = Array(3) { DoubleArray(key.length + maxEdits + 2) }
        val found = ArrayList<Pair<Int, Double>>()
        for (i in keys.indices) {
            val candidate = keys[i]
            if (kotlin.math.abs(candidate.length - key.length) > maxEdits) continue
            // Several forms of one key follow each other; only the most frequent one is used
            if (i > 0 && keys[i - 1] == candidate) continue
            if (candidate == key) continue
            if (own >= 0 && counts[i].toLong() < ownCount * 100) continue
            val cost = editCost(key, candidate, maxCost, buffers)
            if (cost <= maxCost) found.add(i to cost)
        }
        found.sortWith(compareBy<Pair<Int, Double>> { it.second }.thenByDescending { counts[it.first] })
        return found.asSequence().map { words[it.first] }.take(limit).toList()
    }

    /** Weighted edit distance (with swapped neighbours) between [a] and [b]; a large value if it is above [limit]. */
    private fun editCost(a: String, b: String, limit: Double, buffers: Array<DoubleArray>): Double {
        val n = a.length
        val m = b.length
        var prev2 = buffers[0]
        var prev = buffers[1]
        var cur = buffers[2]
        for (j in 0..m) prev[j] = j.toDouble()
        var prevRowMin = 0.0
        for (i in 1..n) {
            cur[0] = i.toDouble()
            var rowMin = cur[0]
            for (j in 1..m) {
                val x = a[i - 1]
                val y = b[j - 1]
                val substitute = if (x == y) 0.0 else if (areNeighbours(x, y)) 0.6 else 1.0
                var best = minOf(prev[j] + 1.0, cur[j - 1] + 1.0, prev[j - 1] + substitute)
                if (i > 1 && j > 1 && x == b[j - 2] && a[i - 2] == y) best = minOf(best, prev2[j - 2] + 1.0)
                cur[j] = best
                if (best < rowMin) rowMin = best
            }
            // Stop early when two rows in a row are already too far away
            if (rowMin > limit && prevRowMin > limit) return Double.MAX_VALUE
            prevRowMin = rowMin
            val free = prev2
            prev2 = prev
            prev = cur
            cur = free
        }
        return prev[m]
    }

    companion object {
        /** Position (row, column) of the letters on the Arabic and English keyboard rows. */
        private val keyPositions: Map<Char, Pair<Int, Int>> = buildMap {
            val rows = listOf(
                "ضصثقفغعهخحج", "شسيبلاتنمكط", "ذؤريهوزظد", // Arabic
                "qwertyuiop", "asdfghjkl", "zxcvbnm", // English
            )
            for ((r, row) in rows.withIndex()) {
                for ((c, ch) in row.withIndex()) putIfAbsent(ch, (r % 3) to c)
            }
        }

        /** True if the two letters sit next to each other on the keyboard. */
        private fun areNeighbours(x: Char, y: Char): Boolean {
            val p = keyPositions[x] ?: return false
            val q = keyPositions[y] ?: return false
            return kotlin.math.abs(p.first - q.first) <= 1 && kotlin.math.abs(p.second - q.second) <= 1
        }

        private val cache = HashMap<String, UiDictionary?>()
        private val pairCache = HashMap<String, Map<String, List<String>>>()

        /**
         * Words that usually follow [previous], from the bundled pair list `assets/ime/dict/<language>_pairs.txt`
         * (`previous<TAB>next<TAB>count`, most frequent first). Empty if there is no such list.
         */
        fun nextWords(context: Context, language: String, previous: String, limit: Int): List<String> {
            val map = synchronized(pairCache) {
                pairCache.getOrPut(language) { runCatching { loadPairs(context, language) }.getOrDefault(emptyMap()) }
            }
            return map[normalize(previous, language)].orEmpty().take(limit)
        }

        private val tripleCache = HashMap<String, Map<String, List<String>>>()

        /** Words that usually follow the two words [previous2] [previous], from `<language>_triples.txt`. */
        fun nextWordsAfterTwo(context: Context, language: String, previous2: String, previous: String, limit: Int): List<String> {
            val map = synchronized(tripleCache) {
                tripleCache.getOrPut(language) { runCatching { loadTriples(context, language) }.getOrDefault(emptyMap()) }
            }
            val key = normalize(previous2, language) + "\t" + normalize(previous, language)
            return map[key].orEmpty().take(limit)
        }

        private fun loadTriples(context: Context, language: String): Map<String, List<String>> {
            val result = HashMap<String, MutableList<String>>()
            context.assets.open("ime/dict/${language}_triples.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val parts = line.split('\t')
                    if (parts.size < 3) continue
                    result.getOrPut(parts[0] + "\t" + parts[1]) { ArrayList(PAIRS_PER_WORD) }
                        .let { if (it.size < PAIRS_PER_WORD) it.add(parts[2]) }
                }
            }
            return result
        }

        private fun loadPairs(context: Context, language: String): Map<String, List<String>> {
            val result = HashMap<String, MutableList<String>>()
            context.assets.open("ime/dict/${language}_pairs.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val parts = line.split('\t')
                    if (parts.size < 2) continue
                    result.getOrPut(parts[0]) { ArrayList(PAIRS_PER_WORD) }.let { if (it.size < PAIRS_PER_WORD) it.add(parts[1]) }
                }
            }
            return result
        }

        private const val PAIRS_PER_WORD = 5

        /** Loads (once) the dictionary for [language] ("ar", "en"); null if no word list is bundled for it. */
        @Synchronized
        fun forLanguage(context: Context, language: String): UiDictionary? {
            return cache.getOrPut(language) {
                runCatching { load(context, language) }.getOrNull()
            }
        }

        private fun load(context: Context, language: String): UiDictionary? {
            val entries = ArrayList<Triple<String, String, Int>>(50000)
            context.assets.open("ime/dict/$language.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val tab = line.indexOf('\t')
                    if (tab <= 0) continue
                    val word = line.substring(0, tab)
                    val count = line.substring(tab + 1).trim().toIntOrNull() ?: continue
                    val key = normalize(word, language)
                    if (key.isNotEmpty()) entries.add(Triple(key, word, count))
                }
            }
            if (entries.isEmpty()) return null
            // Sort by key; for the same key the most frequent form first
            entries.sortWith(compareBy<Triple<String, String, Int>> { it.first }.thenByDescending { it.third })
            return UiDictionary(
                keys = Array(entries.size) { entries[it].first },
                words = Array(entries.size) { entries[it].second },
                counts = IntArray(entries.size) { entries[it].third },
                language = language,
            )
        }

        fun normalize(text: String, language: String): String {
            if (language != "ar") return text.lowercase()
            val sb = StringBuilder(text.length)
            for (ch in text) {
                when (ch) {
                    // Diacritics (tashkeel), superscript alef and tatweel are ignored
                    in '\u064B'..'\u065F', '\u0670', '\u0640' -> {}
                    '\u0623', '\u0625', '\u0622', '\u0671' -> sb.append('\u0627') // alef forms -> alef
                    '\u0649' -> sb.append('\u064A') // alef maqsura -> ya
                    '\u0629' -> sb.append('\u0647') // ta marbuta -> ha
                    else -> sb.append(ch)
                }
            }
            return sb.toString()
        }
    }
}
