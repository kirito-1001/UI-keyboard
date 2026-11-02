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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.editor.OperationUnit
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.keyboardManager

/**
 * The text editing panel which replaces the key area.
 *
 * Visual layout (left to right):
 * ```
 * select-all | up     | clear  | backspace
 * left       | select | right  | back
 * search     | down   | cancel | space (tall)
 * copy       | cut    | paste  | space
 * ```
 */
@Composable
fun EditTextPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val editorInstance by context.editorInstance()
    val state by keyboardManager.activeState.collectAsState()
    val dispatcher = keyboardManager.inputEventDispatcher

    val content by editorInstance.activeContentFlow.collectAsState()
    val clipboardManager by context.clipboardManager()
    val primaryClip by clipboardManager.primaryClipFlow.collectAsState()

    // Keys which cannot do anything right now are dimmed. If the app does not report its text (raw editors), the
    // text dependent keys stay enabled because we cannot know.
    val known = content.localSelection.isValid
    val hasSelection = state.isSelectionMode || content.selection.isSelectionMode
    val selectionModeOn = state.isManualSelectionMode
    val hasText = !known || content.text.isNotEmpty()
    val canLeft = !known || hasSelection || content.offset > 0 || content.textBeforeSelection.isNotEmpty()
    val canRight = !known || hasSelection || content.textAfterSelection.isNotEmpty()
    val canPaste = primaryClip != null

    @Composable
    fun RowScope.Key(
        data: dev.patrickgold.florisboard.ime.keyboard.KeyData,
        enabled: Boolean = true,
        content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
    ) {
        UiDispatchKey(
            keyData = data,
            inputEventDispatcher = dispatcher,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            enabled = enabled,
            content = content,
        )
    }

    @Composable
    fun RowScope.Tap(
        onClick: () -> Unit,
        enabled: Boolean = true,
        showDot: Boolean = false,
        content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
    ) {
        UiTapKey(
            onClick = onClick,
            enabled = enabled,
            showDot = showDot,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            content = content,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardUiHeight()),
    ) {
        // Three left columns
        Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
            // Row 1: select-all | up | clear
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Key(TextKeyData.CLIPBOARD_SELECT_ALL, enabled = hasText) {
                    UiKeyLabel(uiString(R.string.ui__edit__select_all), Icons.Default.SelectAll)
                }
                Key(TextKeyData.ARROW_UP) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
                Tap(
                    onClick = {
                        // Deletes only a selected text, never single characters.
                        if (editorInstance.activeContent.selection.isSelectionMode) {
                            editorInstance.deleteBackwards(OperationUnit.CHARACTERS)
                        }
                    },
                    // Never dimmed (excluded from the enabled/disabled look)
                ) {
                    UiKeyLabel(uiString(R.string.ui__edit__clear), Icons.Default.Delete)
                }
            }
            // Row 2: left | select | right
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Key(TextKeyData.ARROW_LEFT) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Key(TextKeyData.CLIPBOARD_SELECT) {
                    UiKeyLabel(uiString(R.string.ui__edit__select))
                    if (selectionModeOn) ActiveDot()
                }
                Key(TextKeyData.ARROW_RIGHT) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            // Row 3: search | down | cancel
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Tap(
                    onClick = {
                        val text = editorInstance.activeContent.selectedText
                        if (text.isNotBlank()) {
                            val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(text))
                            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // No browser available, nothing to do.
                            }
                        }
                    },
                    enabled = hasSelection,
                ) {
                    UiKeyLabel(uiString(R.string.ui__edit__search), Icons.Default.Search)
                }
                Key(TextKeyData.ARROW_DOWN) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                Tap(
                    onClick = {
                        val selection = editorInstance.activeContent.selection
                        if (selection.isSelectionMode) {
                            editorInstance.setSelection(selection.end, selection.end)
                        }
                        keyboardManager.activeState.isManualSelectionMode = false
                        keyboardManager.activeState.isManualSelectionModeStart = false
                        keyboardManager.activeState.isManualSelectionModeEnd = false
                    },
                    enabled = hasSelection || selectionModeOn,
                ) {
                    UiKeyLabel(uiString(R.string.ui__edit__cancel), UiIcons.CircleX)
                }
            }
            // Row 4: copy | cut | paste
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Key(TextKeyData.CLIPBOARD_COPY, enabled = hasSelection) {
                    UiKeyLabel(uiString(R.string.ui__edit__copy), Icons.Default.ContentCopy)
                }
                Key(TextKeyData.CLIPBOARD_CUT, enabled = hasSelection) {
                    UiKeyLabel(uiString(R.string.ui__edit__cut), Icons.Default.ContentCut)
                }
                Key(TextKeyData.CLIPBOARD_PASTE, enabled = canPaste) {
                    UiKeyLabel(uiString(R.string.ui__edit__paste), UiIcons.PasteTornSheet)
                }
            }
        }
        // Right column: backspace, back, space (tall)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            UiDispatchKey(
                keyData = TextKeyData.DELETE,
                inputEventDispatcher = dispatcher,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enabled = canLeft,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null)
            }
            UiTapKey(
                onClick = { UiPanelState.panel = UiPanel.NONE },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                UiKeyLabel(uiString(R.string.ui__edit__back))
            }
            UiDispatchKey(
                keyData = TextKeyData.SPACE,
                inputEventDispatcher = dispatcher,
                modifier = Modifier.weight(2f).fillMaxSize(),
            ) {
                Icon(Icons.Default.SpaceBar, contentDescription = null)
            }
        }
    }
}
