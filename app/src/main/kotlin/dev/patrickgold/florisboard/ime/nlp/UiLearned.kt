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

    /** How often something was used and when it was used last (minutes since 1970), so recent use ranks higher. */
    private class Entry(var count: Int, var time: Long)

    private class Data {
        val words = HashMap<String, Entry>()
        val pairs = HashMap<String, Entry>() // "previous\u0000word"
        val triples = HashMap<String, Entry>() // "previous2\u0000previous\u0000word"
    }

    private fun nowMinutes() = System.currentTimeMillis() / 60_000L

    /**
     * Ranking value: the count, up to three times higher for something used just now, falling back to the plain count
     * over a few weeks. Something typed once today ranks above something typed twice months ago.
     */
    private fun score(e: Entry): Double {
        val ageDays = ((nowMinutes() - e.time).coerceAtLeast(0)) / 1440.0
        return e.count * (1.0 + 2.0 / (1.0 + ageDays / 7.0))
    }

    /** Reads "count" or "count@minutes" (older files have no time). */
    private fun parseEntry(text: String): Entry? {
        val at = text.indexOf('@')
        val count = (if (at < 0) text else text.substring(0, at)).toIntOrNull() ?: return null
        val time = if (at < 0) 0L else text.substring(at + 1).toLongOrNull() ?: 0L
        return Entry(count, time)
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
                        3 -> parseEntry(parts[2])?.let { data.getOrPut(parts[0]) { Data() }.words[parts[1]] = it }
                        4 -> parseEntry(parts[3])?.let {
                            data.getOrPut(parts[0]) { Data() }.pairs[parts[1] + "\u0000" + parts[2]] = it
                        }
                        5 -> parseEntry(parts[4])?.let {
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
                    for ((word, e) in d.words) w.write("$lang\t$word\t${e.count}@${e.time}\n")
                    for ((pair, e) in d.pairs) w.write("$lang\t${pair.replace('\u0000', '\t')}\t${e.count}@${e.time}\n")
                    for ((triple, e) in d.triples) {
                        w.write("$lang\t${triple.replace('\u0000', '\t')}\t${e.count}@${e.time}\n")
                    }
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
        val now = nowMinutes()
        fun HashMap<String, Entry>.bump(key: String, by: Int = 1) {
            val e = get(key)
            if (e == null) put(key, Entry(by, now)) else { e.count += by; e.time = now }
        }
        d.words.bump(word)
        if (previous != null) d.pairs.bump(previous + "\u0000" + word)
        if (previous != null && previous2 != null) d.triples.bump(previous2 + "\u0000" + previous + "\u0000" + word)
        if (d.triples.size > MAX_PAIRS_PER_LANGUAGE) prune(d.triples, MAX_PAIRS_PER_LANGUAGE * 9 / 10)
        if (d.words.size > MAX_WORDS_PER_LANGUAGE) prune(d.words, MAX_WORDS_PER_LANGUAGE * 9 / 10)
        if (d.pairs.size > MAX_PAIRS_PER_LANGUAGE) prune(d.pairs, MAX_PAIRS_PER_LANGUAGE * 9 / 10)
        dirty = true
        saveIfDue()
    }

    /**
     * The user picked [word] from the suggestions: counts as using it twice, because choosing a suggestion shows
     * that it is wanted. (The normal learning adds the other use when the word is finished.)
     */
    @Synchronized
    fun reward(context: Context, language: String, word: String) {
        ensureLoaded(context)
        if (word.isBlank()) return
        val d = data.getOrPut(language) { Data() }
        val e = d.words[word]
        if (e == null) d.words[word] = Entry(1, nowMinutes()) else { e.count += 1; e.time = nowMinutes() }
        dirty = true
        saveIfDue()
    }

    private fun prune(map: HashMap<String, Entry>, keep: Int) {
        val threshold = map.values.map { it.count }.sorted()[map.size - keep]
        map.entries.removeAll { it.value.count < threshold }
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
            .sortedByDescending { score(it.value) }
            .take(limit)
            .map { it.key to it.value.count }
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
        return d.words.entries.any { it.value.count >= 2 && UiDictionary.normalize(it.key, language) == key }
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
            .map { Triple(it.key.substring(head.length), it.value.count, score(it.value)) }
            .filter { key.isEmpty() || UiDictionary.normalize(it.first, language).startsWith(key) }
            .filter { it.first != prefix }
            .sortedByDescending { it.third }
            .take(limit)
            .map { it.first to it.second }
    }

    /** Words that were typed after the two words [previous2] [previous], most used first. */
    @Synchronized
    fun nextAfterTwo(
        context: Context,
        language: String,
        previous2: String,
        previous: String,
        limit: Int,
        prefix: String = "",
    ): List<Pair<String, Int>> {
        ensureLoaded(context)
        val d = data[language] ?: return emptyList()
        val head = previous2 + "\u0000" + previous + "\u0000"
        val key = UiDictionary.normalize(prefix, language)
        return d.triples.entries
            .filter { it.key.startsWith(head) }
            .map { Triple(it.key.substring(head.length), it.value.count, score(it.value)) }
            .filter { key.isEmpty() || UiDictionary.normalize(it.first, language).startsWith(key) }
            .filter { it.first != prefix }
            .sortedByDescending { it.third }
            .take(limit)
            .map { it.first to it.second }
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
