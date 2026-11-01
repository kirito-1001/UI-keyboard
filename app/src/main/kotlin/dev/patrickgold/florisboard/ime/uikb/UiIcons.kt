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
     * The "edit text" icon: a rounded box with a filled block (selected text) and a text cursor (I-beam) on its right.
     */
    val EditText: ImageVector by lazy {
        ImageVector.Builder(
            name = "EditText",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Box outline
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 8f)
                lineTo(4f, 8f)
                lineTo(2.5f, 9.5f)
                lineTo(2.5f, 14.5f)
                lineTo(4f, 16f)
                lineTo(18f, 16f)
            }
            // Selected-text block
            path(fill = SolidColor(Color.Black)) {
                moveTo(5.5f, 10.5f)
                lineTo(12f, 10.5f)
                lineTo(12f, 13.5f)
                lineTo(5.5f, 13.5f)
                close()
            }
            // I-beam cursor
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Butt,
            ) {
                moveTo(16f, 3f)
                lineTo(20f, 3f)
                moveTo(18f, 3f)
                lineTo(18f, 21f)
                moveTo(16f, 21f)
                lineTo(20f, 21f)
            }
        }.build()
    }
}
