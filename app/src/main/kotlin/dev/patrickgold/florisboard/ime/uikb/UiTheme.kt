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

package dev.patrickgold.florisboard.ime.uikb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.SnyggSinglePropertySet
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import org.florisboard.lib.snygg.ui.LocalSnyggBackdrop
import org.florisboard.lib.snygg.ui.SnyggBackdrop
import org.florisboard.lib.snygg.ui.LocalSnyggStyleTransform
import org.florisboard.lib.snygg.ui.SnyggStyleTransform
import org.florisboard.lib.snygg.ui.withColors
import java.io.File

/** A ready-made color theme: keyboard background, key color and accent (Enter key). */
data class UiThemeDef(val background: Color, val key: Color, val accent: Color)

/** The ready-made themes, in the order they are shown in the settings. */
val UiThemes = listOf(
    UiThemeDef(Color(0xFFEBEFF2), Color(0xFFC9CED1), Color(0xFF4CB6AC)),
    UiThemeDef(Color(0xFF273238), Color(0xFF3B464C), Color(0xFF7FCAC3)),
    UiThemeDef(Color(0xFFF8F9FB), Color(0xFFE1E3E2), Color(0xFF1A73E9)),
    UiThemeDef(Color(0xFF363B3F), Color(0xFF56595E), Color(0xFF5F97F6)),
    UiThemeDef(Color(0xFFC62827), Color(0xFFDE8887), Color(0xFFF44236)),
    UiThemeDef(Color(0xFF43A047), Color(0xFF96CA99), Color(0xFF2D7E32)),
    UiThemeDef(Color(0xFF00695B), Color(0xFF73ACA5), Color(0xFF4CB6AC)),
    UiThemeDef(Color(0xFF1564C0), Color(0xFF7FABDC), Color(0xFF2097F3)),
    UiThemeDef(Color(0xFF80DEEA), Color(0xFFBAECF3), Color(0xFF00ACC2)),
    UiThemeDef(Color(0xFF45289F), Color(0xFF9888C9), Color(0xFF9675CE)),
    UiThemeDef(Color(0xFFAD1457), Color(0xFFD27EA2), Color(0xFFF40057)),
    UiThemeDef(Color(0xFFF9BBD0), Color(0xFFFBDAE5), Color(0xFFE36187)),
    UiThemeDef(Color(0xFFDACDC5), Color(0xFFEBE3E0), Color(0xFFFF7376)),
    UiThemeDef(Color(0xFF4D342F), Color(0xFF9D8F8C), Color(0xFF8C6E63)),
    UiThemeDef(Color(0xFF36474F), Color(0xFF90999E), Color(0xFF78909C)),
    UiThemeDef(Color(0xFF000000), Color(0xFF727272), Color(0xFF80CBC4)),
    UiThemeDef(Color(0xFF161616), Color(0xFF7E7E7E), Color(0xFF003D5C)),
    UiThemeDef(Color(0xFF161616), Color(0xFF7E7E7E), Color(0xFF282828)),
)

/** Dark text on light colors and light text on dark colors. */
fun uiThemeTextColor(background: Color): Color {
    val lum = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (lum > 0.59f) Color(0xFF141418) else Color(0xFFF5F5F5)
}

private val ImageThemeKey = Color.Black.copy(alpha = 0.36f)
private val ImageThemePopup = Color.Black.copy(alpha = 0.50f)
private val ImageThemeAccent = Color(0xFF4CB6AC)
private const val FrostPopupAlpha = 0.55f

/** Without a ready-made theme: only makes the popup background see-through for the frosted glass. */
private object FrostOnlyTransform : SnyggStyleTransform {
    override fun transform(
        elementName: String,
        attributes: SnyggQueryAttributes,
        selector: SnyggSelector?,
        style: SnyggSinglePropertySet,
    ): SnyggSinglePropertySet {
        return if (elementName == FlorisImeUi.KeyPopupBox.elementName) {
            style.withColors(background = style.background(Color.Gray).copy(alpha = FrostPopupAlpha))
        } else {
            style
        }
    }
}

private class UiThemeTransform(
    private val background: Color,
    private val key: Color,
    private val accent: Color,
    private val isImage: Boolean,
    /** Frosted glass behind the popups (no picture, Android 12+): their background has to be see-through. */
    private val frost: Boolean = false,
) : SnyggStyleTransform {
    private val textBg = uiThemeTextColor(if (isImage) Color.Black else background)
    private val textKey = if (isImage) Color.White else uiThemeTextColor(key)
    private val textAccent = uiThemeTextColor(accent)

    override fun transform(
        elementName: String,
        attributes: SnyggQueryAttributes,
        selector: SnyggSelector?,
        style: SnyggSinglePropertySet,
    ): SnyggSinglePropertySet {
        val pressed = selector == SnyggSelector.PRESSED
        return when {
            elementName == FlorisImeUi.Window.elementName ->
                style.withColors(background = background, foreground = textBg)
            elementName == FlorisImeUi.Smartbar.elementName ->
                style.withColors(background = Color.Transparent, foreground = textBg)
            elementName == FlorisImeUi.SmartbarActionsOverflow.elementName ->
                style.withColors(background = Color.Transparent, foreground = textBg)
            elementName.startsWith("smartbar") ->
                style.withColors(foreground = textBg)
            elementName == FlorisImeUi.KeyPopupBox.elementName ->
                style.withColors(
                    background = when {
                        isImage -> ImageThemePopup
                        frost -> lerp(key, textKey, 0.10f).copy(alpha = FrostPopupAlpha)
                        else -> lerp(key, textKey, 0.10f)
                    },
                    foreground = textKey,
                )
            elementName == FlorisImeUi.KeyPopupElement.elementName ->
                if (selector == SnyggSelector.FOCUS) {
                    style.withColors(background = accent, foreground = textAccent)
                } else {
                    style.withColors(background = Color.Transparent, foreground = textKey)
                }
            elementName == FlorisImeUi.KeyPopupExtendedIndicator.elementName ->
                style.withColors(foreground = textKey)
            elementName == FlorisImeUi.KeyHint.elementName ->
                style.withColors(foreground = textKey.copy(alpha = 0.6f))
            elementName == FlorisImeUi.Key.elementName -> {
                val code = attributes[FlorisImeUi.Attr.Code] as? Int
                val capsLock = code == KeyCode.SHIFT &&
                    attributes[FlorisImeUi.Attr.ShiftState] == InputShiftState.CAPS_LOCK.toString()
                if (code == KeyCode.ENTER) {
                    style.withColors(
                        background = if (pressed) lerp(accent, textAccent, 0.2f) else accent,
                        foreground = textAccent,
                    )
                } else {
                    style.withColors(
                        background = if (pressed) lerp(key, textKey, 0.2f) else key,
                        foreground = if (capsLock) null else textKey,
                    )
                }
            }
            else -> style
        }
    }
}

/** Applies the chosen ready-made theme or background picture to everything inside [content]. */
@Composable
fun ProvideUiTheme(content: @Composable () -> Unit) {
    val prefs by FlorisPreferenceStore
    val index by prefs.theme.uiThemeIndex.observeAsState()
    val imagePath by prefs.theme.uiThemeImagePath.observeAsState()
    val frostAvailable = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val transform = remember(index, imagePath) {
        val def = UiThemes.getOrNull(index)
        when {
            def != null -> UiThemeTransform(def.background, def.key, def.accent, isImage = false, frost = frostAvailable)
            imagePath.isNotBlank() && File(imagePath).exists() ->
                UiThemeTransform(Color.Black, ImageThemeKey, ImageThemeAccent, isImage = true)
            else -> null
        }
    }
    val blurred = if (transform != null && index < 0) rememberUiThemeBlur(imagePath) else null
    val backdrop = remember(blurred) {
        blurred?.let { SnyggBackdrop(it, setOf(FlorisImeUi.Key.elementName, FlorisImeUi.KeyPopupBox.elementName)) }
    }
    CompositionLocalProvider(
        LocalSnyggStyleTransform provides (transform ?: if (frostAvailable) FrostOnlyTransform else null),
        LocalSnyggBackdrop provides backdrop,
        content = content,
    )
}

/** Draws the chosen background picture (if any) behind the keys. */
@Composable
fun UiThemeBackgroundImage(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val index by prefs.theme.uiThemeIndex.observeAsState()
    val imagePath by prefs.theme.uiThemeImagePath.observeAsState()
    if (index >= 0 || imagePath.isBlank()) return
    val bitmap = rememberUiThemeImage(imagePath)
    val backdrop = LocalSnyggBackdrop.current
    bitmap?.let {
        Image(
            modifier = modifier.onGloballyPositioned { coords ->
                backdrop?.origin = coords.positionInRoot()
                backdrop?.size = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
            },
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        // Soft shadow under the toolbar: solid behind the toolbar, fading out over the gap above the keys
        val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
        if (smartbarEnabled) {
            val barHeight = FlorisImeSizing.smartbarHeight
            Box(
                modifier = modifier.drawBehind {
                    val bar = barHeight.toPx()
                    val fade = ToolbarShadowFade.toPx()
                    val total = bar + fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = ToolbarShadowAlpha),
                            (bar / total) to Color.Black.copy(alpha = ToolbarShadowAlpha),
                            1f to Color.Transparent,
                            endY = total,
                        ),
                        size = Size(size.width, total),
                    )
                },
            )
        }
    }
}

private const val ToolbarShadowAlpha = 0.35f
private val ToolbarShadowFade = 44.dp

/** Loads the picture at [path] off the main thread. */
@Composable
fun rememberUiThemeImage(path: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path) {
        value = if (path.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    return state.value
}

/**
 * A real gaussian-blurred copy of the picture at [path] (frosted glass behind the keys). It is blurred once, at a
 * reduced size (the blur hides the missing detail), and then scaled up behind the keys.
 */
@Composable
fun rememberUiThemeBlur(path: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path) {
        value = if (path.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val full = BitmapFactory.decodeFile(path) ?: return@runCatching null
                    val scale = BlurWorkSide.toFloat() / maxOf(full.width, full.height)
                    val w = (full.width * scale).toInt().coerceAtLeast(1)
                    val h = (full.height * scale).toInt().coerceAtLeast(1)
                    val small = android.graphics.Bitmap.createScaledBitmap(full, w, h, true)
                    if (small !== full) full.recycle()
                    val pixels = IntArray(w * h)
                    small.getPixels(pixels, 0, w, 0, 0, w, h)
                    // Three box blur passes approximate a gaussian blur
                    repeat(3) { gaussianPass(pixels, w, h, BlurRadius) }
                    small.setPixels(pixels, 0, w, 0, 0, w, h)
                    small.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return state.value
}

private const val BlurWorkSide = 360
private const val BlurRadius = 10

/** One horizontal + one vertical box blur pass over [px] (ARGB, edges clamped). */
private fun gaussianPass(px: IntArray, w: Int, h: Int, r: Int) {
    val tmp = IntArray(px.size)
    boxBlur(px, tmp, w, h, r, horizontal = true)
    boxBlur(tmp, px, w, h, r, horizontal = false)
}

private fun boxBlur(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val len = if (horizontal) w else h
    val div = 2 * r + 1
    for (line in 0 until lines) {
        fun idx(i: Int): Int {
            val c = i.coerceIn(0, len - 1)
            return if (horizontal) line * w + c else c * w + line
        }
        var sr = 0
        var sg = 0
        var sb = 0
        for (i in -r..r) {
            val p = src[idx(i)]
            sr += (p shr 16) and 0xFF
            sg += (p shr 8) and 0xFF
            sb += p and 0xFF
        }
        for (i in 0 until len) {
            dst[idx(i)] = (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val add = src[idx(i + r + 1)]
            val rem = src[idx(i - r)]
            sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
            sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
            sb += (add and 0xFF) - (rem and 0xFF)
        }
    }
}
