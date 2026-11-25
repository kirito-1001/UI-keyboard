/*
 * Copyright 2021 Patrick Goldinger
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

package dev.patrickgold.jetpref.datastore.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluatorScope
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

/**
 * Material list item which behaves and looks like a preference, but does not provide an
 * automatic display of state for a datastore entry, unlike all other available preference
 * composables. This can be used to manually add a custom preference or be used to allow for
 * click-only actions in a preference screen, such as navigation to a sub screen or in the app's
 * about screen.
 *
 * @param modifier Modifier to be applied to the underlying list item.
 * @param icon The [ImageVector] of the list entry icon.
 * @param iconSpaceReserved If the space at the start of the list item should be reserved (blank
 *  space) if no icon ID is provided.
 * @param title The title of this preference, shown as the list item primary text (max 1 line).
 * @param summary The summary of this preference, shown as the list item secondary text (max 2 lines).
 * @param trailing Optional trailing composable, will be placed at the end of the list item.
 * @param enabledIf Evaluator scope which allows to dynamically decide if this preference should be
 *  enabled (true) or disabled (false).
 * @param visibleIf Evaluator scope which allows to dynamically decide if this preference should be
 *  visible (true) or hidden (false).
 * @param onClick The action to perform if this preference is enabled and the user clicks on this
 *  preference item. Mutually exclusive with [eventModifier].
 * @param eventModifier An optional modifier to apply to the preference item. This can be used to set up toggles or
 *  other interactions. Mutually exclusive with [onClick].
 *
 * @since 0.1.0
 */
@Composable
fun Preference(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    summary: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
    onClick: (() -> Unit)? = null,
    eventModifier: (@Composable () -> Modifier)? = null,
    belowContent: (@Composable () -> Unit)? = null,
) {
    require(!(onClick != null && eventModifier != null)) {
        "You cannot provide both an onClick lambda and an eventModifier."
    }
    if (LocalIsPrefVisible.current && visibleIf(PreferenceDataEvaluatorScope)) {
        val isEnabled = LocalIsPrefEnabled.current && enabledIf(PreferenceDataEvaluatorScope)
        val cardColor = LocalPrefCardColor.current
        CompositionLocalProvider(
            LocalIconSpaceReserved provides iconSpaceReserved,
            LocalIsPrefEnabled provides isEnabled,
            LocalIsPrefVisible provides true,
        ) {
            val itemContent: @Composable () -> Unit = {
                JetPrefListItem(
                    modifier = if (onClick != null) {
                        modifier.clickable(
                            enabled = isEnabled,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        modifier.then(eventModifier?.invoke() ?: Modifier)
                    },
                    icon = maybeJetIcon(imageVector = icon, iconSpaceReserved = iconSpaceReserved),
                    text = title,
                    secondaryText = summary,
                    trailing = trailing,
                    enabled = isEnabled,
                    colors = if (cardColor != null) {
                        ListItemDefaults.colors(containerColor = Color.Transparent)
                    } else {
                        ListItemDefaults.colors()
                    },
                )
            }
            if (cardColor != null) {
                // (UI Keyboard patch) every preference is a card; belowContent opens under the item
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = cardColor,
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        itemContent()
                        belowContent?.invoke()
                    }
                }
            } else {
                Column(modifier = Modifier.animateContentSize()) {
                    itemContent()
                    belowContent?.invoke()
                }
            }
        }
    }
}
