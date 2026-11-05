/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.nlp.latin

import android.content.Context
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.UiDictionary
import dev.patrickgold.florisboard.ime.nlp.UiLearned
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.florisboard.lib.android.readText
import org.florisboard.lib.kotlin.guardedByLock

class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.latin"
    }

    private val appContext by context.appContext()

    private val wordData = guardedByLock { mutableMapOf<String, Int>() }
    private val wordDataSerializer = MapSerializer(String.serializer(), Int.serializer())

    override val providerId = ProviderId

    override suspend fun create() {
        // Here we initialize our provider, set up all things which are not language dependent.
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        // Here we have the chance to preload dictionaries and prepare a neural network for a specific language.
        // Is kept in sync with the active keyboard subtype of the user, however a new preload does not necessary mean
        // the previous language is not needed anymore (e.g. if the user constantly switches between two subtypes)

        // To read a file from the APK assets the following methods can be used:
        // appContext.assets.open()
        // appContext.assets.reader()
        // appContext.assets.bufferedReader()
        // appContext.assets.readText()
        // To copy an APK file/dir to the file system cache (appContext.cacheDir), the following methods are available:
        // appContext.assets.copy()
        // appContext.assets.copyRecursively()

        // The subtype we get here contains a lot of data, however we are only interested in subtype.primaryLocale and
        // subtype.secondaryLocales.

        // Load the word list of the language in the background so the first suggestion is instant
        UiDictionary.forLanguage(appContext, subtype.primaryLocale.language)

        wordData.withLock { wordData ->
            if (wordData.isEmpty()) {
                // Here we use readText() because the test dictionary is a json dictionary
                val rawData = appContext.assets.readText("ime/dict/data.json")
                val jsonData = Json.decodeFromString(wordDataSerializer, rawData)
                wordData.putAll(jsonData)
            }
        }
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): SpellingResult {
        return when (word.lowercase()) {
            // Use typo for typing errors
            "typo" -> SpellingResult.typo(arrayOf("typo1", "typo2", "typo3"))
            // Use grammar error if the algorithm can detect this. On Android 11 and lower grammar errors are visually
            // marked as typos due to a lack of support
            "gerror" -> SpellingResult.grammarError(arrayOf("grammar1", "grammar2", "grammar3"))
            // Use valid word for valid input
            else -> SpellingResult.validWord()
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        val language = subtype.primaryLocale.language
        val limit = maxCandidateCount.coerceAtMost(5)
        val typed = content.composingText.ifBlank { content.currentWordText }
        val words = withContext(Dispatchers.Default) {
            if (typed.isBlank()) {
                // Nothing is being typed: learn the last finished word and predict the next one
                val (previous, last) = lastWords(content.textBeforeSelection)
                if (last == null) return@withContext emptyList<String>()
                if (!isPrivateSession) learnOnce(language, previous, last, content.selection.start)
                // What you wrote after this word before comes first, then the common pairs of the language
                val learned = UiLearned.next(appContext, language, last, "", limit).map { it.first }
                (learned + UiDictionary.nextWords(appContext, language, last, limit)).distinct().take(limit)
            } else {
                // A word is being typed: learned words first, then the bundled word list
                val learned = UiLearned.complete(appContext, language, typed, limit).map { it.first }
                val dictionary = UiDictionary.forLanguage(appContext, language)
                val fromList = dictionary?.complete(typed, limit).orEmpty()
                (learned + fromList).distinct().take(limit)
            }
        }
        return words.mapIndexed { index, word ->
            WordSuggestionCandidate(
                text = if (typed.isBlank()) word else matchCase(typed, word),
                confidence = 1.0 - index * 0.1,
                isEligibleForAutoCommit = false,
                isEligibleForUserRemoval = false,
                sourceProvider = this,
            )
        }
    }

    private var lastLearnedKey: String? = null

    /** Learns a finished word once, even if the suggestions are refreshed several times at the same cursor position. */
    private fun learnOnce(language: String, previous: String?, word: String, position: Int) {
        val key = "$language|$previous|$word|$position"
        if (key == lastLearnedKey) return
        lastLearnedKey = key
        UiLearned.learn(appContext, language, previous, word)
    }

    /**
     * Returns the last two finished words before the cursor (previous, last), only if the text ends with a space.
     * Words are lowercased. [last] is null if there is no such word.
     */
    private fun lastWords(textBefore: String): Pair<String?, String?> {
        if (textBefore.isEmpty() || !textBefore.last().isWhitespace()) return null to null
        val tokens = textBefore.takeLast(120).split(Regex("[^\\p{L}\\p{M}']+")).filter { it.isNotEmpty() }
        val last = tokens.lastOrNull()?.takeIf { it.length in 2..30 || it == "a" || it == "i" } ?: return null to null
        val previous = tokens.getOrNull(tokens.size - 2)?.takeIf { it.length in 1..30 }
        // Text ending in punctuation (not a plain word + space) is not learned
        if (!textBefore.trimEnd().last().isLetter()) return null to null
        val breaks = textBefore.trimEnd().dropLast(last.length).takeLast(3).any { it in ".!?؟\n" }
        return (if (breaks) null else previous?.lowercase()) to last.lowercase()
    }

    /** Gives [word] the capitalization the user typed [typed] with (Abc -> Word, ABC -> WORD). */
    private fun matchCase(typed: String, word: String): String {
        if (typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } && typed.any { it.isUpperCase() }) {
            return word.uppercase()
        }
        if (typed.first().isUpperCase()) return word.replaceFirstChar { it.uppercase() }
        return word
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // We can use flogDebug, flogInfo, flogWarning and flogError for debug logging, which is a wrapper for Logcat
        flogDebug { candidate.toString() }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { candidate.toString() }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        flogDebug { candidate.toString() }
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return wordData.withLock { it.keys.toList() }
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return wordData.withLock { it.getOrDefault(word, 0) / 255.0 }
    }

    override suspend fun destroy() {
        // Here we have the chance to de-allocate memory and finish our work. However this might never be called if
        // the app process is killed (which will most likely always be the case).
    }
}
