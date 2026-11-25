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

package dev.patrickgold.florisboard.app.settings.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.media.emoji.emojiSizeFactor
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.LocalPrefCardColor
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

private val PreviewEmojis = listOf("😀", "😍", "😂", "👍", "🎉")

/**
 * First option of the emoji settings: five emoji size levels (1 = default and smallest).
 */
@Composable
fun EmojiSizePreference() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val level by prefs.emoji.sizeLevel.observeAsState()
    val cardColor = LocalPrefCardColor.current ?: MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                text = stringRes(R.string.ui__emoji_size__title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringRes(R.string.ui__emoji_size__summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (emoji in PreviewEmojis) {
                    Text(text = emoji, fontSize = (22f * emojiSizeFactor(level)).sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "😀", fontSize = 16.sp)
                Slider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    value = level.toFloat(),
                    onValueChange = { v ->
                        val newLevel = v.toInt().coerceIn(1, 5)
                        if (newLevel != level) {
                            scope.launch { prefs.emoji.sizeLevel.set(newLevel) }
                        }
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                )
                Text(text = "😀", fontSize = 28.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(
                    onClick = { scope.launch { prefs.emoji.sizeLevel.set(1) } },
                    enabled = level != 1,
                ) {
                    Text(stringRes(R.string.ui__emoji_size__default))
                }
            }
        }
    }
}
