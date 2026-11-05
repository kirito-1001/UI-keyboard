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

package dev.patrickgold.florisboard.app.settings.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard.KeyCustomization
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.keyboard.normalizeKeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

/** A key which can be customized: its normalized code and the label shown in the editor. */
private data class EditableKey(val code: Int, val label: String)

private fun friendlyLabel(code: Int, label: String): String = when (code) {
    KeyCode.SHIFT -> "⇧"
    KeyCode.DELETE -> "⌫"
    KeyCode.ENTER -> "⏎"
    KeyCode.SPACE -> "␣"
    KeyCode.LANGUAGE_SWITCH -> "🌐"
    KeyCode.VIEW_SYMBOLS -> "?123"
    KeyCode.VIEW_CHARACTERS -> "ABC"
    KeyCode.VIEW_SYMBOLS2 -> "=\\<"
    KeyCode.IME_UI_MODE_MEDIA -> "☺"
    else -> label.ifBlank { "#$code" }
}

private val ColorPresets: List<Int> = listOf(
    0xFF43D17A.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFB0BEC5.toInt(), 0xFF455A64.toInt(),
    0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(),
    0xFF00ACC1.toInt(), 0xFF1E88E5.toInt(), 0xFF5E35B1.toInt(), 0xFFD81B60.toInt(),
)

/**
 * Settings screen which lets the user remove keys, move keys inside their row and change their colors.
 */
@Composable
fun KeyCustomizationScreen() = FlorisScreen {
    title = stringRes(R.string.settings__key_customization__title)
    previewFieldVisible = true

    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val store by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val customizations by store.keyboard.keyCustomizations.observeAsState()
    val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()

    var rows by remember { mutableStateOf<List<List<EditableKey?>>>(emptyList()) }
    var selected by remember { mutableStateOf<EditableKey?>(null) }

    LaunchedEffect(activeSubtype, customizations) {
        val keyboard = keyboardManager.layoutManager
            .computeKeyboardAsync(KeyboardMode.CHARACTERS, activeSubtype).await()
        rows = keyboard.arrangement.map { row ->
            row.map { key ->
                val data = key.data as? KeyData
                if (data == null || data.code == KeyCode.UNSPECIFIED) {
                    null
                } else {
                    EditableKey(normalizeKeyCode(data.code), friendlyLabel(data.code, data.label))
                }
            }
        }
    }

    fun update(code: Int, transform: (KeyCustomization) -> KeyCustomization) {
        scope.launch { store.keyboard.keyCustomizations.set(customizations.update(code, transform)) }
    }

    content {
        // All keys of a row share the width of the screen, so nothing is cut off and no scrolling is needed
        val maxKeys = rows.maxOfOrNull { row -> row.count { it != null } } ?: 1
        for (row in rows) {
            val keys = row.filterNotNull()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (key in keys) {
                    val custom = customizations.find(key.code)
                    val isSelected = selected == key
                    // The key being edited is also shaded, so it is easy to see which key is selected
                    val background = custom?.background?.let { Color(it) }
                        ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    val foreground = custom?.foreground?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .clickable { selected = key },
                        shape = RoundedCornerShape(8.dp),
                        color = background,
                        // The key being edited is highlighted with a thick colored border
                        border = if (isSelected) {
                            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key.label,
                                fontSize = 15.sp,
                                maxLines = 1,
                                color = if (custom?.hidden == true) foreground.copy(alpha = 0.4f) else foreground,
                                textDecoration = if (custom?.hidden == true) TextDecoration.LineThrough else null,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Shorter rows keep the same key width as the longest row
                if (keys.size < maxKeys) {
                    Spacer(modifier = Modifier.weight((maxKeys - keys.size).toFloat()))
                }
            }
        }

        OutlinedButton(
            onClick = { scope.launch { store.keyboard.keyCustomizations.set(customizations.copy(items = emptyList())) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(text = stringRes(R.string.settings__key_customization__reset_all))
        }

        // Editing tools for the selected key, shown below the keyboard
        val key = selected
        if (key == null) {
            Text(
                text = stringRes(R.string.settings__key_customization__hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            val custom = customizations.find(key.code) ?: KeyCustomization(key.code)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "${stringRes(R.string.settings__key_customization__key)}: ${key.label}",
                    style = MaterialTheme.typography.titleMedium,
                )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringRes(R.string.settings__key_customization__hide),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = custom.hidden,
                            onCheckedChange = { checked -> update(key.code) { it.copy(hidden = checked) } },
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringRes(R.string.settings__key_customization__position),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { update(key.code) { it.copy(moveBy = it.moveBy - 1) } }) {
                            Text("◀")
                        }
                        OutlinedButton(onClick = { update(key.code) { it.copy(moveBy = it.moveBy + 1) } }) {
                            Text("▶")
                        }
                    }
                    ColorChooser(
                        label = stringRes(R.string.settings__key_customization__background),
                        current = custom.background,
                        onChange = { color -> update(key.code) { it.copy(background = color) } },
                    )
                    ColorChooser(
                        label = stringRes(R.string.settings__key_customization__text_color),
                        current = custom.foreground,
                        onChange = { color -> update(key.code) { it.copy(foreground = color) } },
                    )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { update(key.code) { KeyCustomization(it.code) } }) {
                        Text(stringRes(R.string.settings__key_customization__reset_key))
                    }
                    TextButton(onClick = { selected = null }) {
                        Text(stringRes(R.string.action__ok))
                    }
                }
            }
        }
    }

}

@Composable
private fun ColorChooser(
    label: String,
    current: Int?,
    onChange: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "Default" swatch: removes the override
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onChange(null) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (current == null) 3.dp else 1.dp,
                    if (current == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) { Text("∅") }
            }
            for (preset in ColorPresets) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(preset), CircleShape)
                        .clickable { onChange(preset) },
                ) {
                    if (current == preset) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Transparent, CircleShape)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✓", color = if (preset == 0xFF000000.toInt()) Color.White else Color.Black)
                        }
                    }
                }
            }
        }
    }
}
