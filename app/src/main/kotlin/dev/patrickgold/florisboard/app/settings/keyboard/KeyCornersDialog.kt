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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

/**
 * Content shown under the "Key corners" / "Popup corners" option of Settings > Keyboard when it is opened: a slider for
 * the corner roundness (0% square, 100% capsule) and a reset button which gives the control back to the theme.
 */
@Composable
fun KeyCornersContent(popup: Boolean = false) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val pref = if (popup) prefs.keyboard.popupCornerPercent else prefs.keyboard.keyCornerPercent
    val saved by pref.observeAsState()
    var sliderValue by remember { mutableFloatStateOf(if (saved < 0) 30f else saved.toFloat()) }
    // -1 (theme) stays until the slider is touched
    var useTheme by remember { androidx.compose.runtime.mutableStateOf(saved < 0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(
                onClick = {
                    useTheme = true
                    scope.launch { pref.set(-1) }
                },
            ) { Text(stringRes(R.string.ui__key_corners__reset)) }
        }
    }
}

/**
 * Content shown under the "Hint size" option of Settings > Keyboard when it is opened: sliders for the size and the
 * fade of the key hints and a button which sets both back to the default (5).
 */
@Composable
fun HintSizeContent() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val sizePref = prefs.keyboard.hintSize
    val fadePref = prefs.keyboard.hintFade
    val hintSize by sizePref.observeAsState()
    val hintFade by fadePref.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringRes(R.string.ui__hint_size__size))
            Text(hintSize.toString())
        }
        Slider(
            value = hintSize.toFloat(),
            onValueChange = { v -> scope.launch { sizePref.set(v.roundToInt()) } },
            valueRange = 1f..10f,
            steps = 8,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringRes(R.string.ui__hint_size__fade))
            Text(hintFade.toString())
        }
        Slider(
            value = hintFade.toFloat(),
            onValueChange = { v -> scope.launch { fadePref.set(v.roundToInt()) } },
            valueRange = 1f..10f,
            steps = 8,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        sizePref.set(5)
                        fadePref.set(5)
                    }
                },
            ) { Text(stringRes(R.string.ui__hint_size__reset)) }
        }
    }
}
