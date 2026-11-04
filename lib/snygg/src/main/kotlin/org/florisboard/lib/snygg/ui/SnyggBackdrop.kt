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

package org.florisboard.lib.snygg.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A blurred picture that is shown behind all elements named in [elements] (frosted glass effect). The picture is
 * laid out like a "crop" background image over the area at [origin] with [size] (both in root coordinates).
 */
class SnyggBackdrop(val image: ImageBitmap, val elements: Set<String>) {
    var origin by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(Size.Zero)
}

val LocalSnyggBackdrop: ProvidableCompositionLocal<SnyggBackdrop?> = compositionLocalOf { null }

@Composable
internal fun Modifier.snyggBackdrop(elementName: String?, shape: Shape): Modifier {
    val backdrop = LocalSnyggBackdrop.current
    if (backdrop == null || elementName == null || elementName !in backdrop.elements) return this
    var position by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { position = it.positionInRoot() }
        .drawBehind {
            val area = backdrop.size
            if (area.width <= 0f || area.height <= 0f || size.width <= 0f || size.height <= 0f) return@drawBehind
            val image = backdrop.image
            val scale = max(area.width / image.width, area.height / image.height)
            val dstW = image.width * scale
            val dstH = image.height * scale
            val imageLeft = backdrop.origin.x + (area.width - dstW) / 2f
            val imageTop = backdrop.origin.y + (area.height - dstH) / 2f
            val outline = shape.createOutline(size, layoutDirection, this)
            val clip = when (outline) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            }
            clipPath(clip) {
                translate(left = imageLeft - position.x, top = imageTop - position.y) {
                    drawImage(
                        image = image,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(image.width, image.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }
        }
}
