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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Panels which can temporarily replace the key area of the text keyboard.
 */
enum class UiPanel {
    NONE,
    EDIT_TEXT,
    NUMERIC,
}

/**
 * Small UI-only state of the "UI keyboard" additions. Lives only while the process lives.
 */
object UiPanelState {
    /** The panel currently shown instead of the letter keys. */
    var panel by mutableStateOf(UiPanel.NONE)

    /** True while the language popup menu (opened by long-pressing the language box) is visible. */
    var languageMenuOpen by mutableStateOf(false)

    /** Index of the first visible menu entry (the menu shows up to [MENU_VISIBLE] entries at once). */
    var menuScroll by mutableIntStateOf(0)

    /** Visible slot (0 until [MENU_VISIBLE]) under the finger, or -1. */
    var menuFingerSlot by mutableIntStateOf(-1)

    /** Index of the menu entry currently under the finger, or -1. */
    val languageMenuHighlight: Int
        get() {
            val visible = minOf(MENU_VISIBLE, menuEntries.size)
            return if (menuFingerSlot in 0 until visible) menuScroll + menuFingerSlot else -1
        }

    const val MENU_VISIBLE = 4

    /** Entries of the language popup menu, filled when the menu is opened. */
    var menuEntries by mutableStateOf(emptyList<UiMenuEntry>())

    /** Height of one menu item in pixels, set by the menu overlay when it is drawn. */
    var menuItemHeightPx by mutableIntStateOf(1)

    /** Toggles [target]: shows it, or goes back to the letters if it is already shown. */
    fun toggle(target: UiPanel) {
        panel = if (panel == target) UiPanel.NONE else target
    }
}

/** One entry of the language popup menu. */
class UiMenuEntry(val label: String, val action: () -> Unit)
