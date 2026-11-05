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

    companion object {
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
