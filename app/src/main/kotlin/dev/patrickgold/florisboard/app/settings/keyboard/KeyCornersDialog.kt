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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

/**
 * Dialog of Settings > Keyboard > Key corners: a slider for the corner roundness of the keys (0% square, 100% capsule),
 * and a reset button which gives the control back to the theme.
 */
@Composable
fun KeyCornersDialog(onDismiss: () -> Unit, popup: Boolean = false) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val pref = if (popup) prefs.keyboard.popupCornerPercent else prefs.keyboard.keyCornerPercent
    val saved by pref.observeAsState()
    var sliderValue by remember { mutableFloatStateOf(if (saved < 0) 30f else saved.toFloat()) }
    // -1 (theme) stays until the slider is touched
    var useTheme by remember { androidx.compose.runtime.mutableStateOf(saved < 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringRes(if (popup) R.string.ui__popup_corners__title else R.string.ui__key_corners__title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (useTheme) {
                        stringRes(R.string.ui__key_corners__value_theme)
                    } else {
                        stringRes(R.string.ui__key_corners__value, "v" to sliderValue.toInt())
                    },
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        useTheme = false
                        scope.launch { pref.set(it.toInt()) }
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringRes(R.string.ui__key_corners__min))
                    Text(stringRes(R.string.ui__key_corners__max))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringRes(R.string.ui__key_corners__done)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    useTheme = true
                    scope.launch { pref.set(-1) }
                },
            ) { Text(stringRes(R.string.ui__key_corners__reset)) }
        },
    )
}
