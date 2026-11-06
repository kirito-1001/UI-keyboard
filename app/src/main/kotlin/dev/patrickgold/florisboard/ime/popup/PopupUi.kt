/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

val GlobalStateNumPopupsShowing = MutableStateFlow(0)

@Composable
fun PopupBaseBox(
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes,
    key: Key,
    shouldIndicateExtendedPopups: Boolean,
): Unit = with(LocalDensity.current) {
    DisposableEffect(key) {
        GlobalStateNumPopupsShowing.update { it + 1 }
        onDispose {
            GlobalStateNumPopupsShowing.update { it - 1 }
        }
    }

    SnyggBox(
        elementName = FlorisImeUi.KeyPopupBox.elementName,
        attributes = attributes,
        modifier = modifier,
    ) {
        key.label?.let { label ->
            SnyggBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(key.visibleBounds.height.toDp())
                    .align(Alignment.TopCenter),
            ) {
                SnyggText(
                    modifier = Modifier.align(Alignment.Center),
                    text = label,
                )
            }
        }
        if (shouldIndicateExtendedPopups) {
            SnyggIcon(
                elementName = FlorisImeUi.KeyPopupExtendedIndicator.elementName,
                attributes = attributes,
                modifier = Modifier.align(Alignment.CenterEnd),
                imageVector = Icons.Default.MoreHoriz,
            )
        }
    }
}

@Composable
fun PopupExtBox(
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes,
    elements: List<List<PopupUiController.Element>>,
    elemArrangement: Arrangement.Horizontal,
    elemWidth: Dp,
    elemHeight: Dp,
    activeElementIndex: Int,
    contentScale: Float = 1.0f,
): Unit = with(LocalDensity.current) {
    // Thin dividers between the characters, in the text colour of the popup elements
    val dividerColor = rememberSnyggThemeQuery(FlorisImeUi.KeyPopupElement.elementName, attributes)
        .foreground(Color.Gray).copy(alpha = 0.55f)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val rowsTopToBottom = elements.asReversed()
    SnyggColumn(FlorisImeUi.KeyPopupBox.elementName, attributes, modifier = modifier) {
        for ((rowIndex, row) in rowsTopToBottom.withIndex()) {
            val isLastRow = rowIndex == rowsTopToBottom.lastIndex
            SnyggRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(elemHeight),
                horizontalArrangement = elemArrangement,
            ) {
                for ((elementIndex, element) in row.withIndex()) {
                    val isLastInRow = elementIndex == row.lastIndex
                    val selector = if (activeElementIndex == element.orderedIndex) {
                        SnyggSelector.FOCUS
                    } else {
                        null
                    }
                    val localAttrs = attributes.plus(FlorisImeUi.Attr.Code to element.data.code)
                    SnyggBox(
                        elementName = FlorisImeUi.KeyPopupElement.elementName,
                        attributes = localAttrs,
                        selector = selector,
                        modifier = Modifier.size(elemWidth, elemHeight).drawBehind {
                            val stroke = 1.dp.toPx()
                            val inset = size.height * 0.14f
                            if (!isLastInRow) {
                                val x = if (isLtr) size.width - stroke / 2 else stroke / 2
                                drawLine(dividerColor, Offset(x, inset), Offset(x, size.height - inset), stroke)
                            }
                            if (!isLastRow) {
                                val y = size.height - stroke / 2
                                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), stroke)
                            }
                        },
                    ) {
                        element.label?.let { label ->
                            SnyggText(
                                modifier = Modifier.align(Alignment.Center).graphicsLayer(scaleX = contentScale, scaleY = contentScale),
                                text = label,
                            )
                        }
                        element.icon?.let { icon ->
                            SnyggIcon(
                                modifier = Modifier.align(Alignment.Center).graphicsLayer(scaleX = contentScale, scaleY = contentScale),
                                imageVector = icon,
                            )
                        }
                    }
                }
            }
        }
    }
}
