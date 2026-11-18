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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggText

/**
 * The panel opened by the "Cards and passkeys" action (after the fingerprint / screen lock check). It lists the saved
 * cards and website logins in two tabs; tapping a value types it into the field (without using the clipboard).
 * Adding, editing and deleting is done in Settings.
 */
@Composable
fun VaultPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val editorInstance by context.editorInstance()
    var tab by remember { mutableIntStateOf(0) }

    // Closing the panel (or the keyboard) locks the data again
    DisposableEffect(Unit) {
        onDispose { UiVault.lockIme() }
    }

    val cards = vaultCardViews(UiVault.data.cards)
    val logins = vaultLoginViews(UiVault.data.logins)
    val entries = if (tab == 0) cards else logins

    val label: @Composable (String, Boolean) -> Unit = { text, _ ->
        SnyggText(elementName = FlorisImeUi.SmartbarCandidateWordText.elementName, text = text)
    }

    SnyggBox(
        elementName = FlorisImeUi.SmartbarActionsOverflow.elementName,
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardUiHeight()),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(31.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabButton(uiString(R.string.ui__vault__tab_cards), tab == 0, Modifier.weight(1f), label) { tab = 0 }
                TabButton(uiString(R.string.ui__vault__tab_logins), tab == 1, Modifier.weight(1f), label) { tab = 1 }
                // Settings (opens the cards section) and back, as round buttons
                RoundButton(UiIcons.Gear) {
                    dev.patrickgold.florisboard.FlorisImeService.launchSettings("ui://florisboard/settings/vault")
                }
                RoundButton(Icons.AutoMirrored.Filled.ArrowForward) {
                    UiPanelState.panel = UiPanel.NONE
                }
            }
            Box(modifier = Modifier.fillMaxSize().padding(top = 6.dp)) {
                if (UiVault.unreadable) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        label(uiString(R.string.ui__vault__unreadable), false)
                    }
                } else if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        label(uiString(R.string.ui__vault__empty), false)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entries, key = { it.id }) { entry ->
                            VaultEntryCard(
                                entry = entry,
                                label = label,
                                onField = { value ->
                                    UiVault.lastInsertAt = android.os.SystemClock.uptimeMillis()
                                    // No automatic space before the value (it would break a password or a number)
                                    editorInstance.phantomSpace.setInactive()
                                    editorInstance.commitText(value)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    label: @Composable (String, Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(25.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFF43D17A).copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        label(text, selected)
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFF4A4D50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
