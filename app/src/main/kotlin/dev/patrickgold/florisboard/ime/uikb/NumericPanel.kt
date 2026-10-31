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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.subtypeManager
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggText

private val EnterKeyData = TextKeyData(code = KeyCode.ENTER, type = KeyType.ENTER_EDITING, label = "enter")

private const val ArabicIndicZero = '٠'

/** Converts the ASCII digit [digit] (0-9) to the digit shown for the given language code. */
internal fun localizedDigit(digit: Int, language: String): String {
    return if (language == "ar") (ArabicIndicZero + digit).toString() else digit.toString()
}

/**
 * The numeric pad which replaces the key area.
 *
 * Visual layout (left to right): a scrolling column of symbols, a 3x4 digit grid and the right column with
 * backspace, back, space and enter. The digit grid is read right to left, so `1` is in the top right corner.
 */
@Composable
fun NumericPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val editorInstance by context.editorInstance()
    val subtypeManager by context.subtypeManager()
    val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
    val dispatcher = keyboardManager.inputEventDispatcher

    val language = activeSubtype.primaryLocale.language
    val isArabic = language == "ar"
    val decimalSeparator = if (isArabic) "٫" else ","
    val symbols = listOf(".", "+", "-", "×", "÷", decimalSeparator, "=", "%", "(", ")")

    val panelHeight: Dp = FlorisImeSizing.keyboardUiHeight()

    fun commit(text: String) {
        editorInstance.commitText(text)
    }

    @Composable
    fun RowScope.DigitKey(label: String) {
        UiTapKey(
            onClick = { commit(label) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            SnyggText(text = label)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight),
    ) {
        // Left: five symbols visible at once, scrolls vertically
        LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(symbols.size) { index ->
                val symbol = symbols[index]
                UiTapKey(
                    onClick = { commit(symbol) },
                    modifier = Modifier.fillMaxWidth().height(panelHeight / 5),
                ) {
                    SnyggText(text = symbol)
                }
            }
        }
        // Middle: digits. Reading order is right to left, so each row is laid out mirrored.
        Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
            val rows = listOf(
                listOf(1, 2, 3),
                listOf(4, 5, 6),
                listOf(7, 8, 9),
            )
            for (row in rows) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (digit in row.reversed()) {
                        DigitKey(localizedDigit(digit, language))
                    }
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Right to left: * 0 #  ->  left to right: # 0 *
                DigitKey("#")
                DigitKey(localizedDigit(0, language))
                DigitKey("*")
            }
        }
        // Right: backspace, back, space, enter
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            UiDispatchKey(
                keyData = TextKeyData.DELETE,
                inputEventDispatcher = dispatcher,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null)
            }
            UiTapKey(
                onClick = { UiPanelState.panel = UiPanel.NONE },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                UiKeyLabel(stringRes(R.string.ui__edit__back))
            }
            UiDispatchKey(
                keyData = TextKeyData.SPACE,
                inputEventDispatcher = dispatcher,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Icon(Icons.Default.SpaceBar, contentDescription = null)
            }
            UiDispatchKey(
                keyData = EnterKeyData,
                inputEventDispatcher = dispatcher,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null)
            }
        }
    }
}
