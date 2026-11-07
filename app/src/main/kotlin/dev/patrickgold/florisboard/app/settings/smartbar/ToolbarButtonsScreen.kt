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

package dev.patrickgold.florisboard.app.settings.smartbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard.DefaultComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickAction
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionArrangement
import dev.patrickgold.florisboard.ime.smartbar.quickaction.computeDisplayName
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

/**
 * Settings screen which lets the user reorder, hide and show the toolbar (smartbar) buttons.
 */
@Composable
fun ToolbarButtonsScreen() = FlorisScreen {
    title = stringRes(R.string.settings__toolbar_buttons__title)
    previewFieldVisible = true

    val store by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val arrangement by store.smartbar.actionArrangement.observeAsState()

    androidx.compose.runtime.LaunchedEffect(arrangement) {
        val updated = arrangement.withThemesAction().withVoiceAction()
        if (updated != arrangement) store.smartbar.actionArrangement.set(updated)
    }

    fun save(newArrangement: QuickActionArrangement) {
        scope.launch { store.smartbar.actionArrangement.set(newArrangement.distinct()) }
    }

    content {
        Text(
            text = stringRes(R.string.settings__toolbar_buttons__hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        val dynamic = arrangement.dynamicActions
        for ((index, action) in dynamic.withIndex()) {
            ToolbarButtonRow(
                action = action,
                isVisible = true,
                canMoveUp = index > 0,
                canMoveDown = index < dynamic.size - 1,
                onMoveUp = {
                    val list = dynamic.toMutableList()
                    list.add(index - 1, list.removeAt(index))
                    save(arrangement.copy(dynamicActions = list))
                },
                onMoveDown = {
                    val list = dynamic.toMutableList()
                    list.add(index + 1, list.removeAt(index))
                    save(arrangement.copy(dynamicActions = list))
                },
                onToggleVisible = {
                    save(
                        arrangement.copy(
                            dynamicActions = dynamic.filterIndexed { i, _ -> i != index },
                            hiddenActions = arrangement.hiddenActions + action,
                        )
                    )
                },
            )
        }

        if (arrangement.hiddenActions.isNotEmpty()) {
            Text(
                text = stringRes(R.string.settings__toolbar_buttons__hidden_group),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            for (action in arrangement.hiddenActions) {
                ToolbarButtonRow(
                    action = action,
                    isVisible = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onToggleVisible = {
                        save(
                            arrangement.copy(
                                dynamicActions = arrangement.dynamicActions + action,
                                hiddenActions = arrangement.hiddenActions.filter { it != action },
                            )
                        )
                    },
                )
            }
        }

        OutlinedButton(
            onClick = { save(QuickActionArrangement.Default) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(text = stringRes(R.string.settings__toolbar_buttons__reset))
        }
    }
}

@Composable
private fun ToolbarButtonRow(
    action: QuickAction,
    isVisible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisible: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val imageVector = if (action is QuickAction.InsertKey) {
            DefaultComputingEvaluator.computeImageVector(action.data)
        } else {
            null
        }
        if (imageVector != null) {
            Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(24.dp))
        } else {
            Text(text = "•", modifier = Modifier.size(24.dp))
        }
        Text(
            text = action.computeDisplayName(DefaultComputingEvaluator),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        if (isVisible) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null)
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null)
            }
        }
        IconButton(onClick = onToggleVisible) {
            Icon(
                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null,
            )
        }
    }
}
