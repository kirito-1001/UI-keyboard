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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.subtypeManager
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

/**
 * The toolbar of the "UI keyboard", shown while there are no word suggestions.
 *
 * Left to right: logo (opens the settings), edit-text panel, language box, emoji, hide keyboard.
 */
@Composable
fun UiToolbar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    val dispatcher = keyboardManager.inputEventDispatcher
    val barHeight = FlorisImeSizing.smartbarHeight

    fun languageName(locale: java.util.Locale): String {
        val name = locale.getDisplayLanguage(locale)
        return name.replaceFirstChar { it.uppercase() }
    }

    val labelNumeric = uiString(R.string.ui__menu__numeric)
    val labelSymbols = uiString(R.string.ui__menu__symbols)
    val labelSettings = uiString(R.string.ui__menu__settings)

    fun buildEntries(): List<UiMenuEntry> = buildList {
        for (subtype in subtypes) {
            add(UiMenuEntry(languageName(subtype.primaryLocale.base)) {
                UiPanelState.panel = UiPanel.NONE
                subtypeManager.switchToSubtypeById(subtype.id)
            })
        }
        add(UiMenuEntry(labelNumeric) { UiPanelState.panel = UiPanel.NUMERIC })
        add(UiMenuEntry(labelSymbols) {
            UiPanelState.panel = UiPanel.NONE
            dispatcher.sendDownUp(TextKeyData.VIEW_SYMBOLS)
        })
        add(UiMenuEntry(labelSettings) { dispatcher.sendDownUp(TextKeyData.SETTINGS) })
    }

    @Composable
    fun RowScope.BarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
        SnyggIconButton(
            elementName = FlorisImeUi.SmartbarActionKey.elementName,
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 4.dp).height(barHeight).aspectRatio(1f),
        ) {
            content()
        }
    }

    SnyggRow(
        modifier = modifier.fillMaxWidth().height(barHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(onClick = { dispatcher.sendDownUp(TextKeyData.SETTINGS) }) {
            SnyggIcon(painter = painterResource(R.drawable.ic_ui_keyboard_logo))
        }
        BarButton(onClick = { UiPanelState.toggle(UiPanel.EDIT_TEXT) }) {
            SnyggIcon(imageVector = Icons.Default.EditNote)
        }

        // Language box: tap switches to the next language, long press opens the popup menu.
        SnyggBox(
            elementName = FlorisImeUi.SmartbarActionKey.elementName,
            selector = if (UiPanelState.languageMenuOpen) SnyggSelector.PRESSED else SnyggSelector.NONE,
            modifier = Modifier.weight(1f).height(barHeight).padding(horizontal = 6.dp, vertical = 4.dp),
            clickAndSemanticsModifier = Modifier.pointerInput(subtypes) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        // Released (or moved) before the long press time: counts as a tap.
                        subtypeManager.switchToNextSubtype()
                        UiPanelState.panel = UiPanel.NONE
                        return@awaitEachGesture
                    }
                    UiPanelState.menuEntries = buildEntries()
                    UiPanelState.menuScroll = 0
                    UiPanelState.menuFingerSlot = -1
                    UiPanelState.languageMenuOpen = true
                    val startY = longPress.position.y
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val slot = UiPanelState.menuItemHeightPx
                        val y = change.position.y
                        if (!moved && kotlin.math.abs(y - startY) > slot / 3f) moved = true
                        // Item 0 starts one slot above the toolbar, item 1 is level with the toolbar, and so on.
                        val index = kotlin.math.floor((y + slot) / slot).toInt()
                        UiPanelState.menuFingerSlot = if (moved && index in 0 until UiPanelState.MENU_VISIBLE) index else -1
                        val released = !change.pressed
                        change.consume()
                        if (released) break
                    }
                    val chosen = UiPanelState.menuEntries.getOrNull(UiPanelState.languageMenuHighlight)
                    UiPanelState.languageMenuOpen = false
                    UiPanelState.menuFingerSlot = -1
                    chosen?.action?.invoke()
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            SnyggText(text = languageName(activeSubtype.primaryLocale.base))
        }

        BarButton(onClick = {
            UiPanelState.panel = UiPanel.NONE
            dispatcher.sendDownUp(TextKeyData.IME_UI_MODE_MEDIA)
        }) {
            SnyggIcon(imageVector = Icons.Default.EmojiEmotions)
        }
        BarButton(onClick = { dispatcher.sendDownUp(TextKeyData.IME_HIDE_UI) }) {
            SnyggIcon(imageVector = Icons.Default.KeyboardArrowDown)
        }
    }
}

/**
 * The popup menu of the language box. It is drawn on the left side, starting one item above the toolbar, so the items
 * are split between the area above and below the toolbar. Only [UiPanelState.MENU_VISIBLE] entries are visible at
 * once: the two middle ones normally, the first and last faded. Holding the finger on a faded entry scrolls the list.
 * Must be placed at the top-start corner of the keyboard view.
 */
@Composable
fun UiLanguageMenu(modifier: Modifier = Modifier) {
    val entries = UiPanelState.menuEntries
    if (!UiPanelState.languageMenuOpen || entries.isEmpty()) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bar = FlorisImeSizing.smartbarHeight
    val ui = FlorisImeSizing.keyboardUiHeight()
    val visible = minOf(UiPanelState.MENU_VISIBLE, entries.size)
    val maxScroll = entries.size - visible
    val slot = minOf(bar, (bar + ui) / maxOf(visible - 1, 1))
    SideEffect { UiPanelState.menuItemHeightPx = with(density) { slot.roundToPx() }.coerceAtLeast(1) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(350)
            if (UiPanelState.menuFingerSlot == 0 && UiPanelState.menuScroll > 0) {
                UiPanelState.menuScroll -= 1
            } else if (UiPanelState.menuFingerSlot == visible - 1 && UiPanelState.menuScroll < maxScroll) {
                UiPanelState.menuScroll += 1
            }
        }
    }
    Column(
        modifier = modifier
            .absoluteOffset(x = 8.dp, y = -slot)
            .width(170.dp),
    ) {
        for (slotIndex in 0 until visible) {
            val index = UiPanelState.menuScroll + slotIndex
            val entry = entries.getOrNull(index) ?: continue
            val fadeTop = slotIndex == 0 && UiPanelState.menuScroll > 0
            val fadeBottom = slotIndex == visible - 1 && UiPanelState.menuScroll < maxScroll
            SnyggBox(
                elementName = FlorisImeUi.Key.elementName,
                selector = if (index == UiPanelState.languageMenuHighlight) SnyggSelector.PRESSED else SnyggSelector.NONE,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(slot)
                    .padding(vertical = 4.dp)
                    .alpha(if (fadeTop || fadeBottom) 0.35f else 1f),
                contentAlignment = Alignment.Center,
            ) {
                SnyggText(text = entry.label)
            }
        }
    }
}
