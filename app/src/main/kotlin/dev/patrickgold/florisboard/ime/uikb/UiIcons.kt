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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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

    /**
     * The emoji icon: only a ring (3dp thick at the normal 28dp icon size), the two eyes and the mouth are drawn, the
     * rest is empty. Drawn in a 24 x 24 viewport (3dp of 28dp = 2.571 units).
     */
    val EmojiOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "EmojiOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Ring
            addPath(
                pathData = addPathNodes(
                    "M1.286,12 a10.714,10.714 0 1,0 21.428,0 a10.714,10.714 0 1,0 -21.428,0 Z",
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.571f,
            )
            // Eyes
            addPath(
                pathData = addPathNodes(
                    "M7.35,9.4 a1.45,1.45 0 1,0 2.9,0 a1.45,1.45 0 1,0 -2.9,0 Z " +
                        "M13.75,9.4 a1.45,1.45 0 1,0 2.9,0 a1.45,1.45 0 1,0 -2.9,0 Z",
                ),
                fill = SolidColor(Color.Black),
            )
            // Mouth
            addPath(
                pathData = addPathNodes("M7.65,14.63 A4.8,4.8 0 0 0 16.35,14.63"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
            )
        }.build()
    }

    /**
     * The "font" settings icon: a rounded square with a transparent (cut out) letter A. Drawn in a 24 x 24 viewport.
     */
    val FontSquare: ImageVector by lazy {
        ImageVector.Builder(
            name = "FontSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6,2 H18 A4,4 0 0 1 22,6 V18 A4,4 0 0 1 18,22 H6 A4,4 0 0 1 2,18 V6 A4,4 0 0 1 6,2 Z " +
                        "M6.2,18 L10.3,6 H13.7 L17.8,18 H15 L14.2,15.5 H9.8 L9,18 Z " +
                        "M10.5,13.2 L12,8.6 L13.5,13.2 Z"
                ),
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.Black),
            )
        }.build()
    }

    /**
     * The mono app logo shown in the toolbar (keyboard outline with the "U" shield). Drawn in a 200 x 200 viewport.
     */
    val Logo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Logo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 200f,
            viewportHeight = 200f,
        ).apply {
            val paths = listOf(
                "M62,70 H42 A20,20 0 0 0 22,90 V154 A20,20 0 0 0 42,174 H158 A20,20 0 0 0 178,154 V90 A20,20 0 0 0 158,70 H138",
                "M62,18 H138 V76 C138,98 118,114 100,124 C82,114 62,98 62,76 Z",
                "M82,42 V76 C82,87 90,93 100,93 C110,93 118,87 118,76 V42",
            )
            for (data in paths) {
                addPath(
                    pathData = addPathNodes(data),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            // The five key dots
            for (cx in listOf(52f, 76f, 100f, 124f, 148f)) {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(cx - 5f, 148f)
                    arcTo(5f, 5f, 0f, true, false, cx + 5f, 148f)
                    arcTo(5f, 5f, 0f, true, false, cx - 5f, 148f)
                    close()
                }
            }
        }.build()
    }
}
