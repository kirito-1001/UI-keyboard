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
import java.io.File

/**
 * What the keyboard learned from the user's typing: how often each word was used and which word followed which.
 * Everything stays in a private file on the phone (`ui_learned.txt`); nothing is sent anywhere.
 */
object UiLearned {
    private const val FILE_NAME = "ui_learned.txt"
    private const val MAX_WORDS_PER_LANGUAGE = 20000
    private const val MAX_PAIRS_PER_LANGUAGE = 40000
    private const val SAVE_EVERY_MS = 10_000L

    private class Data {
        val words = HashMap<String, Int>()
        val pairs = HashMap<String, Int>() // "previous\u0000word" -> count
        val triples = HashMap<String, Int>() // "previous2\u0000previous\u0000word" -> count
    }

    private val data = HashMap<String, Data>()
    private var file: File? = null
    private var loaded = false
    private var dirty = false
    private var lastSave = 0L

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val f = File(context.filesDir, FILE_NAME)
        file = f
        if (!f.exists()) return
        runCatching {
            f.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val parts = line.split('\t')
                    // "lang<TAB>word<TAB>count" or "lang<TAB>previous<TAB>word<TAB>count"
                    when (parts.size) {
                        3 -> parts[2].toIntOrNull()?.let { data.getOrPut(parts[0]) { Data() }.words[parts[1]] = it }
                        4 -> parts[3].toIntOrNull()?.let {
                            data.getOrPut(parts[0]) { Data() }.pairs[parts[1] + "\u0000" + parts[2]] = it
                        }
                        5 -> parts[4].toIntOrNull()?.let {
                            data.getOrPut(parts[0]) { Data() }.triples[parts[1] + "\u0000" + parts[2] + "\u0000" + parts[3]] = it
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun saveIfDue(force: Boolean = false) {
        val f = file ?: return
        val now = System.currentTimeMillis()
        if (!dirty || (!force && now - lastSave < SAVE_EVERY_MS)) return
        runCatching {
            val tmp = File(f.parentFile, "$FILE_NAME.tmp")
            tmp.bufferedWriter(Charsets.UTF_8).use { w ->
                for ((lang, d) in data) {
                    for ((word, count) in d.words) w.write("$lang\t$word\t$count\n")
                    for ((pair, count) in d.pairs) w.write("$lang\t${pair.replace('\u0000', '\t')}\t$count\n")
                    for ((triple, count) in d.triples) w.write("$lang\t${triple.replace('\u0000', '\t')}\t$count\n")
                }
            }
            tmp.renameTo(f)
            dirty = false
            lastSave = now
        }
    }

    /** Writes pending changes to disk now (called when the keyboard goes away). */
    fun flush(context: Context) {
        ensureLoaded(context)
        saveIfDue(force = true)
    }

    /** Remembers that [word] was typed, after [previous] (if any). */
    @Synchronized
    fun learn(context: Context, language: String, previous2: String?, previous: String?, word: String) {
        ensureLoaded(context)
        val d = data.getOrPut(language) { Data() }
        d.words[word] = (d.words[word] ?: 0) + 1
        if (previous != null) {
            val key = previous + "\u0000" + word
            d.pairs[key] = (d.pairs[key] ?: 0) + 1
        }
        if (previous != null && previous2 != null) {
            val key = previous2 + "\u0000" + previous + "\u0000" + word
            d.triples[key] = (d.triples[key] ?: 0) + 1
        }
        if (d.triples.size > MAX_PAIRS_PER_LANGUAGE) prune(d.triples, MAX_PAIRS_PER_LANGUAGE * 9 / 10)
        if (d.words.size > MAX_WORDS_PER_LANGUAGE) prune(d.words, MAX_WORDS_PER_LANGUAGE * 9 / 10)
        if (d.pairs.size > MAX_PAIRS_PER_LANGUAGE) prune(d.pairs, MAX_PAIRS_PER_LANGUAGE * 9 / 10)
        dirty = true
        saveIfDue()
    }

    private fun prune(map: HashMap<String, Int>, keep: Int) {
        val threshold = map.values.sorted()[map.size - keep]
        map.entries.removeAll { it.value < threshold }
    }

    /** Learned words starting with [prefix] (compared in normalized form), most used first. */
    @Synchronized
    fun complete(context: Context, language: String, prefix: String, limit: Int): List<Pair<String, Int>> {
        ensureLoaded(context)
        val d = data[language] ?: return emptyList()
        val key = UiDictionary.normalize(prefix, language)
        if (key.isEmpty()) return emptyList()
        return d.words.entries
            .filter { UiDictionary.normalize(it.key, language).startsWith(key) && it.key != prefix }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    /**
     * True if [word] was typed at least twice, so it is a word the user really uses and not a typo that was typed
     * once (compared in normalized form).
     */
    @Synchronized
    fun isKnown(context: Context, language: String, word: String): Boolean {
        ensureLoaded(context)
        val d = data[language] ?: return false
        val key = UiDictionary.normalize(word, language)
        if (key.isEmpty()) return false
        return d.words.entries.any { it.value >= 2 && UiDictionary.normalize(it.key, language) == key }
    }

    /** Words that were typed after [previous], optionally only those starting with [prefix], most used first. */
    @Synchronized
    fun next(context: Context, language: String, previous: String, prefix: String, limit: Int): List<Pair<String, Int>> {
        ensureLoaded(context)
        val d = data[language] ?: return emptyList()
        val head = previous + "\u0000"
        val key = UiDictionary.normalize(prefix, language)
        return d.pairs.entries
            .filter { it.key.startsWith(head) }
            .map { it.key.substring(head.length) to it.value }
            .filter { key.isEmpty() || UiDictionary.normalize(it.first, language).startsWith(key) }
            .filter { it.first != prefix }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /** Words that were typed after the two words [previous2] [previous], most used first. */
    @Synchronized
    fun nextAfterTwo(context: Context, language: String, previous2: String, previous: String, limit: Int): List<Pair<String, Int>> {
        ensureLoaded(context)
        val d = data[language] ?: return emptyList()
        val head = previous2 + "\u0000" + previous + "\u0000"
        return d.triples.entries
            .filter { it.key.startsWith(head) }
            .map { it.key.substring(head.length) to it.value }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /** Forgets everything that was learned. */
    @Synchronized
    fun clear(context: Context) {
        ensureLoaded(context)
        data.clear()
        dirty = false
        runCatching { file?.delete() }
    }
}
