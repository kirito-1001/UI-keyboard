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
        lastPrivate = isPrivateSession
        val limit = maxCandidateCount.coerceAtMost(5)
        val typed = content.composingText.ifBlank { content.currentWordText }
        val words = withContext(Dispatchers.Default) {
            if (typed.isBlank()) {
                // Nothing is being typed: learn the last finished word and predict the next one
                val (previous2, previous, last) = lastWords(content.textBeforeSelection)
                if (last == null) return@withContext emptyList<String>()
                if (!isPrivateSession) learnOnce(language, previous2, previous, last, content.selection.start)
                // What you wrote after this word before comes first, then the common pairs of the language
                // The last two words give the best guess, the last word alone is the fallback
                val twoWords = previous != null
                val learned = ((if (twoWords) UiLearned.nextAfterTwo(appContext, language, previous!!, last, limit) else emptyList()) +
                    UiLearned.next(appContext, language, last, "", limit)).map { it.first }.distinct()
                val listed = ((if (twoWords) UiDictionary.nextWordsAfterTwo(appContext, language, previous!!, last, limit) else emptyList()) +
                    UiDictionary.nextWords(appContext, language, last, limit)).distinct()
                interleave(learned, listed).take(limit)
            } else {
                // A word is being typed. Slots from left to right:
                // 1. the closest correct word, if the typed word is not a known word (a typo such as "tha" -> "the")
                // 2. the word you use most (learned from your typing)
                // 3. the word suggested from the bundled word list
                // The words before the one being typed make the guess better: what usually follows them comes first
                val head = content.textBeforeSelection.dropLastWhile {
                    it.isLetter() || it == '\'' || it.category == CharCategory.NON_SPACING_MARK
                }
                val (_, ctx1, ctxLast) = lastWords(head)
                val contextLearned = ArrayList<String>()
                val contextListed = ArrayList<String>()
                if (ctxLast != null) {
                    if (ctx1 != null) {
                        UiLearned.nextAfterTwo(appContext, language, ctx1, ctxLast, limit, typed).mapTo(contextLearned) { it.first }
                        contextListed.addAll(UiDictionary.nextWordsAfterTwo(appContext, language, ctx1, ctxLast, limit, typed))
                    }
                    UiLearned.next(appContext, language, ctxLast, typed, limit).mapTo(contextLearned) { it.first }
                    contextListed.addAll(UiDictionary.nextWords(appContext, language, ctxLast, limit, typed))
                }
                val learned = (contextLearned + UiLearned.complete(appContext, language, typed, limit).map { it.first })
                    .distinct()
                val dictionary = UiDictionary.forLanguage(appContext, language)
                val fromList = (contextListed + dictionary?.complete(typed, limit).orEmpty()).distinct()
                val corrections = if (dictionary != null && typed.all { it.isLetter() } &&
                    !UiLearned.isKnown(appContext, language, typed)
                ) {
                    dictionary.corrections(typed, limit)
                } else {
                    emptyList()
                }
                val result = LinkedHashSet<String>()
                corrections.firstOrNull()?.let { result.add(it) }
                result.addAll(interleave(learned, fromList))
                result.addAll(corrections)
                result.toList().take(limit)
            }
        }
        val allowed = words.filterNot { UiLearned.isBlocked(appContext, language, it) }
        return allowed.mapIndexed { index, word ->
            WordSuggestionCandidate(
                text = if (typed.isBlank()) word else matchCase(typed, word),
                confidence = 1.0 - index * 0.1,
                isEligibleForAutoCommit = false,
                isEligibleForUserRemoval = true,
                sourceProvider = this,
            )
        }
    }

    /** Slot 1 = the best word learned from the user, slot 2 = the best word of the word list, then the rest. */
    private fun interleave(learned: List<String>, listed: List<String>): List<String> {
        val result = LinkedHashSet<String>()
        learned.firstOrNull()?.let { result.add(it) }
        listed.firstOrNull { it !in result }?.let { result.add(it) }
        result.addAll(learned)
        result.addAll(listed)
        return result.toList()
    }

    private var lastLearnedKey: String? = null

    /** Learns a finished word once, even if the suggestions are refreshed several times at the same cursor position. */
    private fun learnOnce(language: String, previous2: String?, previous: String?, word: String, position: Int) {
        val key = "$language|$previous2|$previous|$word|$position"
        if (key == lastLearnedKey) return
        lastLearnedKey = key
        UiLearned.learn(appContext, language, previous2, previous, word)
    }

    /** The last three finished words of the current sentence (oldest first), lowercased; missing ones are null. */
    private var lastPrivate = false

    private data class Recent(val previous2: String?, val previous: String?, val last: String?)

    /**
     * Returns the last finished words before the cursor, only if the text ends with a space after a letter.
     * Words from an earlier sentence are not used, as they are not related.
     */
    private fun lastWords(textBefore: String): Recent {
        val none = Recent(null, null, null)
        if (textBefore.isEmpty() || !textBefore.last().isWhitespace()) return none
        // Text that is only spaces has no finished word
        if (textBefore.trimEnd().lastOrNull()?.isLetter() != true) return none
        val sentence = textBefore.takeLast(200).split(Regex("[.!?؟\\n]+")).last()
        val tokens = sentence.split(Regex("[^\\p{L}\\p{M}']+")).filter { it.isNotEmpty() }.takeLast(3)
        val last = tokens.lastOrNull()?.takeIf { it.length in 2..30 || it == "a" || it == "i" } ?: return none
        val previous = tokens.getOrNull(tokens.size - 2)?.takeIf { it.length in 1..30 }
        val previous2 = if (previous != null) tokens.getOrNull(tokens.size - 3)?.takeIf { it.length in 1..30 } else null
        return Recent(previous2?.lowercase(), previous?.lowercase(), last.lowercase())
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
        // A word picked from the suggestions counts more than a typed one (never in a private session)
        if (candidate is WordSuggestionCandidate && !lastPrivate) {
            UiLearned.reward(appContext, subtype.primaryLocale.language, candidate.text.toString().lowercase())
        }
        flogDebug { candidate.toString() }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { candidate.toString() }
    }

    /** A suggestion was held for 3 seconds: the word is wrong; forget it and never suggest it again. */
    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        if (candidate !is WordSuggestionCandidate) return false
        UiLearned.block(appContext, subtype.primaryLocale.language, candidate.text.toString())
        return true
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
