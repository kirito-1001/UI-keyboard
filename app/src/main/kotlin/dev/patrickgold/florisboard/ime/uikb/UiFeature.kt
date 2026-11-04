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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.ui.graphics.vector.ImageVector
import dev.patrickgold.florisboard.R

/**
 * Features which can be assigned to the two customizable toolbar buttons.
 */
enum class UiFeature(
    @StringRes val labelRes: Int,
    val icon: ImageVector?,
    @DrawableRes val iconRes: Int? = null,
) {
    EMOJI(R.string.ui__feature__emoji, null, R.drawable.ui_icon_emoji),
    EDIT_TEXT(R.string.ui__feature__edit_text, null, R.drawable.ui_icon_edit_text),
    CLIPBOARD(R.string.ui__feature__clipboard, null, R.drawable.ui_icon_clipboard),
    VOICE(R.string.ui__feature__voice, null, R.drawable.ui_icon_voice),
    THEMES(R.string.ui__feature__themes, Icons.Default.ColorLens),
}
