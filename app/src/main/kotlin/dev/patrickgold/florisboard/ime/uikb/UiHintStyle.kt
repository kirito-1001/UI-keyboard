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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.snygg.ui.LocalSnyggFontOverride
import org.florisboard.lib.snygg.ui.LocalSnyggStyleTransform
import org.florisboard.lib.snygg.ui.SnyggFontOverride
import org.florisboard.lib.snygg.ui.SnyggStyleTransform
import org.florisboard.lib.snygg.ui.withColors

/**
 * Applies the "hint size" settings (size and fade, 1..10, default 5 = the original look) to the hints drawn in the
 * top corner of the keys. Size: 1.2 sp per step around the default 12 sp. Fade: 5 keeps the original opacity, lower
 * values make the hint clearer, higher values fainter.
 */
@Composable
fun UiHintStyle(content: @Composable () -> Unit) {
    val prefs by FlorisPreferenceStore
    val size by prefs.keyboard.hintSize.observeAsState()
    val fade by prefs.keyboard.hintFade.observeAsState()
    if (size == 5 && fade == 5) {
        content()
        return
    }
    val parentTransform = LocalSnyggStyleTransform.current
    val parentFont = LocalSnyggFontOverride.current
    val transform = remember(parentTransform, fade) {
        val factor = if (fade >= 5) 1f - (fade - 5) * 0.17f else 1f + (5 - fade) * 0.25f
        SnyggStyleTransform { elementName, attributes, selector, style ->
            val base = parentTransform?.transform(elementName, attributes, selector, style) ?: style
            if (elementName == FlorisImeUi.KeyHint.elementName) {
                val color = base.foreground()
                if (color.alpha > 0f) {
                    base.withColors(foreground = color.copy(alpha = (color.alpha * factor).coerceIn(0.05f, 1f)))
                } else {
                    base
                }
            } else {
                base
            }
        }
    }
    val font = remember(parentFont, size) {
        SnyggFontOverride(parentFont?.fontFamily, (parentFont?.fontSizeDelta ?: 0f) + (size - 5) * 1.2f)
    }
    CompositionLocalProvider(
        LocalSnyggStyleTransform provides transform,
        LocalSnyggFontOverride provides font,
        content = content,
    )
}
