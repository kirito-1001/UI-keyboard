/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.uikb

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.ui.SnyggText

private val VoiceGreen = Color(0xFF43D17A)
private val VoiceGrey = Color(0xFF4A4D50)

/**
 * The built-in voice typing: uses Android's speech recognizer and writes the spoken text into the field. It is
 * shown in the place of the toolbar (see [UiVoiceBar]). The state is kept here, outside of the UI.
 */
object UiVoice {
    enum class Mode { OFF, LISTENING, PAUSED }

    var mode by mutableStateOf(Mode.OFF)
        private set

    /** A string resource id shown instead of the normal status text (a problem, or "microphone permission"). */
    var messageRes by mutableStateOf<Int?>(null)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var appContext: Context? = null
    private var language: String = "ar-SA"

    /** Number of characters of the not yet final phrase that we have written to the field. */
    private var partialLength = 0
    private var mutedStreams: List<Int> = emptyList()

    // The recognizer plays a start/stop sound every time it restarts; the sound streams are muted while listening.
    private fun muteSounds() {
        val ctx = appContext ?: return
        if (mutedStreams.isNotEmpty()) return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        mutedStreams = listOf(AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_MUSIC)
            .filter { stream ->
                runCatching {
                    if (am.isStreamMute(stream)) {
                        false
                    } else {
                        am.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                        true
                    }
                }.getOrDefault(false)
            }
    }

    private fun unmuteSounds() {
        val ctx = appContext ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        mutedStreams.forEach { stream ->
            runCatching { am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) }
        }
        mutedStreams = emptyList()
    }

    /** Writes the phrase being spoken: replaces what was written for it before. */
    private fun writePhrase(text: String, final: Boolean) {
        val ic = FlorisImeService.currentInputConnection() ?: return
        ic.beginBatchEdit()
        // The keyboard may have marked the last word before the cursor as "composing"; committing text would then
        // replace that word as well. Releasing it first makes sure only our own phrase is replaced.
        ic.finishComposingText()
        if (partialLength > 0) ic.deleteSurroundingText(partialLength, 0)
        val out = if (final) "$text " else text
        ic.commitText(out, 1)
        ic.endBatchEdit()
        partialLength = if (final) 0 else out.length
    }

    val isActive: Boolean get() = mode != Mode.OFF

    /** Starts listening, in the language of the active keyboard language. */
    fun begin(context: Context, languageCode: String) {
        appContext = context.applicationContext
        language = when (languageCode) {
            "ar" -> "ar-SA"
            "en" -> "en-US"
            else -> languageCode
        }
        mode = Mode.LISTENING
        messageRes = null
        startListening(userInitiated = true)
    }

    /** Tap on the microphone: pause, or continue. */
    fun toggle() {
        when (mode) {
            Mode.LISTENING -> {
                mode = Mode.PAUSED
                unmuteSounds()
                // Finishes the current phrase (the result is still written to the field)
                recognizer?.stopListening()
            }
            Mode.PAUSED -> {
                mode = Mode.LISTENING
                messageRes = null
                startListening(userInitiated = true)
            }
            Mode.OFF -> Unit
        }
    }

    /** The arrow: leaves the listening mode and brings the toolbar back. */
    fun exit() {
        if (mode == Mode.OFF) return
        mode = Mode.OFF
        messageRes = null
        handler.removeCallbacksAndMessages(null)
        unmuteSounds()
        partialLength = 0
        recognizer?.let {
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }

    /**
     * [userInitiated]: started by the user (the button, or continue after a pause): its start sound is played, and
     * the sounds are muted shortly after. The automatic restarts after each phrase are always silent.
     */
    private fun startListening(userInitiated: Boolean = false) {
        val ctx = appContext ?: return
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mode = Mode.PAUSED
            unmuteSounds()
            messageRes = R.string.ui__voice__need_permission
            runCatching {
                ctx.startActivity(
                    Intent(ctx, UiVoicePermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            mode = Mode.PAUSED
            unmuteSounds()
            messageRes = R.string.ui__voice__unavailable
            return
        }
        if (userInitiated) {
            handler.postDelayed({
                if (mode == Mode.LISTENING) muteSounds()
            }, 800)
        } else {
            muteSounds()
        }
        partialLength = 0
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(ctx).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        // Android 13+: tells the recognizer that the other language (Arabic <-> English) may also be spoken
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val other = if (language.startsWith("ar")) "en-US" else "ar-SA"
            intent.putExtra(RecognizerIntent.EXTRA_ADDITIONAL_LANGUAGES, arrayOf(other))
        }
        r.startListening(intent)
    }

    /** Listens again after a phrase ended (the recognizer stops by itself after a silence). */
    private fun restartSoon(delayMs: Long = 200) {
        handler.postDelayed({
            if (mode == Mode.LISTENING) startListening()
        }, delayMs)
    }

    private fun firstResult(results: Bundle?): String? {
        return results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults) ?: return
            writePhrase(text, final = false)
        }

        override fun onResults(results: Bundle?) {
            val text = firstResult(results)
            if (text != null) {
                writePhrase(text, final = true)
            } else {
                partialLength = 0
            }
            restartSoon()
        }

        override fun onError(error: Int) {
            // What was written for the unfinished phrase stays in the field as it is
            partialLength = 0
            if (mode != Mode.LISTENING) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restartSoon()
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT -> {
                    recognizer?.destroy()
                    recognizer = null
                    restartSoon(500)
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    mode = Mode.PAUSED
                    unmuteSounds()
                    messageRes = R.string.ui__voice__need_permission
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER -> {
                    mode = Mode.PAUSED
                    unmuteSounds()
                    messageRes = R.string.ui__voice__network
                }
                else -> {
                    mode = Mode.PAUSED
                    unmuteSounds()
                    messageRes = R.string.ui__voice__unavailable
                }
            }
        }
    }
}

/** The bar shown instead of the toolbar while the built-in voice typing is on. */
@Composable
fun UiVoiceBar(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) {
        onDispose { UiVoice.exit() }
    }
    val listening = UiVoice.mode == UiVoice.Mode.LISTENING
    val pillShape = RoundedCornerShape(50)
    val transition = rememberInfiniteTransition(label = "voice")
    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring1",
    )
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearOutSlowInEasing),
            RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(900),
        ),
        label = "ring2",
    )
    val status = UiVoice.messageRes?.let { stringResource(it) }
        ?: stringResource(if (listening) R.string.ui__voice__listening else R.string.ui__voice__paused)

    // The bar is the same in every language: microphone on the left, the exit arrow on the right
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.width(84.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                if (listening) {
                    for (t in listOf(t1, t2)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1f + 0.3f * t
                                    scaleY = 1f + 0.45f * t
                                    alpha = 0.7f * (1f - t)
                                }
                                .border(3.dp, VoiceGreen, pillShape),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(pillShape)
                        .background(if (listening) VoiceGreen else VoiceGrey)
                        .clickable { UiVoice.toggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ui_icon_voice),
                        contentDescription = null,
                        modifier = Modifier.height(FlorisImeSizing.smartbarHeight * 0.5f),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                SnyggText(elementName = FlorisImeUi.SmartbarCandidateWordText.elementName, text = status)
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .size(FlorisImeSizing.smartbarHeight - 10.dp)
                    .clip(CircleShape)
                    .background(VoiceGrey)
                    .clickable { UiVoice.exit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}
