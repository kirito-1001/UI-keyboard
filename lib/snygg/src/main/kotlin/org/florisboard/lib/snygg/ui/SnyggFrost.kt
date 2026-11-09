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

package org.florisboard.lib.snygg.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/**
 * Real frosted glass without a background picture (Android 12+): the content of the node that carries
 * [snyggFrostSource] is recorded into [content] and a blurred copy of it is kept in [blurred]. The elements named in
 * [elements] draw that blurred copy behind themselves, so what is behind them (keys, toolbar) looks frosted.
 *
 * Elements that use the blurred copy must not be part of the recorded content (see [snyggFrostExclude]), otherwise
 * they would draw a picture of themselves.
 */
class SnyggFrost(
    val elements: Set<String>,
    val content: GraphicsLayer,
    val blurred: GraphicsLayer,
) {
    /** Position of the recorded node in root coordinates. */
    var origin: Offset = Offset.Zero

    /** True only while the content is being recorded. */
    var recording: Boolean = false
}

val LocalSnyggFrost: ProvidableCompositionLocal<SnyggFrost?> = compositionLocalOf { null }

/** Returns null where the blur is not available (before Android 12). */
@Composable
fun rememberSnyggFrost(elements: Set<String>): SnyggFrost? {
    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return remember(elements, content, blurred) { SnyggFrost(elements, content, blurred) }
}

/** Marks the node whose content is the glass material: it is recorded, blurred, and then drawn normally. */
fun Modifier.snyggFrostSource(frost: SnyggFrost?): Modifier {
    if (frost == null) return this
    return this
        .onGloballyPositioned { frost.origin = it.positionInRoot() }
        .drawWithContent {
            frost.recording = true
            frost.content.record { this@drawWithContent.drawContent() }
            frost.recording = false
            val radius = 14.dp.toPx()
            frost.blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            frost.blurred.record { drawLayer(frost.content) }
            drawContent()
        }
}

/** Elements that draw the blurred copy hide themselves while the content is recorded. */
@Composable
fun Modifier.snyggFrostExclude(): Modifier {
    val frost = LocalSnyggFrost.current ?: return this
    return this.drawWithContent {
        if (!frost.recording) drawContent()
    }
}
