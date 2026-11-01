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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom drawn icons of the UI keyboard.
 */
object UiIcons {
    /**
     * A single sheet of paper torn in two halves along a jagged line. Used for the paste key.
     */
    val PasteTornSheet: ImageVector by lazy {
        ImageVector.Builder(
            name = "PasteTornSheet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Upper half, jagged lower edge
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 10.5f)
                lineTo(5f, 3f)
                lineTo(19f, 3f)
                lineTo(19f, 10.5f)
                lineTo(16.5f, 8.7f)
                lineTo(14f, 10.5f)
                lineTo(11.5f, 8.7f)
                lineTo(9f, 10.5f)
                lineTo(6.5f, 8.7f)
                close()
            }
            // Lower half, jagged upper edge, slightly moved down so the tear is visible
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 13.5f)
                lineTo(6.5f, 15.3f)
                lineTo(9f, 13.5f)
                lineTo(11.5f, 15.3f)
                lineTo(14f, 13.5f)
                lineTo(16.5f, 15.3f)
                lineTo(19f, 13.5f)
                lineTo(19f, 21f)
                lineTo(5f, 21f)
                close()
            }
        }.build()
    }

    /**
     * A circle with an X inside, used for the "cancel selection" key.
     */
    val CircleX: ImageVector by lazy {
        ImageVector.Builder(
            name = "CircleX",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Circle drawn with four arcs
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, true, true, 12f, 21f)
                arcTo(9f, 9f, 0f, true, true, 12f, 3f)
                close()
                // The X
                moveTo(8.5f, 8.5f)
                lineTo(15.5f, 15.5f)
                moveTo(15.5f, 8.5f)
                lineTo(8.5f, 15.5f)
            }
        }.build()
    }

    /**
     * The "edit text" icon: a rounded box outline (open around the cursor), a filled block (selected text) inside it and
     * a text cursor (I-beam with flat serifs). Drawn in a 380 x 380 viewport.
     */
    val EditText: ImageVector by lazy {
        ImageVector.Builder(
            name = "EditText",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 380f,
            viewportHeight = 380f,
        ).apply {
            // Left part of the box outline
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 17f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(214f, 118.5f)
                lineTo(82f, 118.5f)
                arcTo(21.5f, 21.5f, 0f, false, false, 60.5f, 140f)
                lineTo(60.5f, 233f)
                arcTo(21.5f, 21.5f, 0f, false, false, 82f, 254.5f)
                lineTo(214f, 254.5f)
                // Right bracket
                moveTo(250f, 118.5f)
                lineTo(273f, 118.5f)
                arcTo(21.5f, 21.5f, 0f, false, true, 294.5f, 140f)
                lineTo(294.5f, 233f)
                arcTo(21.5f, 21.5f, 0f, false, true, 273f, 254.5f)
                lineTo(250f, 254.5f)
            }
            // Selected-text block, stem and serifs of the cursor
            path(fill = SolidColor(Color.Black)) {
                moveTo(92f, 148f)
                lineTo(182f, 148f)
                lineTo(182f, 226f)
                lineTo(92f, 226f)
                close()
                moveTo(211f, 76f)
                lineTo(232f, 76f)
                lineTo(232f, 300f)
                lineTo(211f, 300f)
                close()
                moveTo(199f, 76f)
                lineTo(244f, 76f)
                lineTo(244f, 93f)
                lineTo(199f, 93f)
                close()
                moveTo(199f, 283f)
                lineTo(244f, 283f)
                lineTo(244f, 300f)
                lineTo(199f, 300f)
                close()
            }
        }.build()
    }

    /**
     * The microphone icon: a filled capsule, a U-shaped holder and a short stem (no base line).
     */
    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.6f, 6.9f)
                arcTo(3.4f, 3.4f, 0f, false, true, 15.4f, 6.9f)
                lineTo(15.4f, 12.1f)
                arcTo(3.4f, 3.4f, 0f, false, true, 8.6f, 12.1f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Holder
                moveTo(5.6f, 11.5f)
                arcTo(6.4f, 6.4f, 0f, false, false, 18.4f, 11.5f)
                // Short stem
                moveTo(12f, 17.7f)
                lineTo(12f, 19.5f)
            }
        }.build()
    }
}
