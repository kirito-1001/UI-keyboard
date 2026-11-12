/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.SideEffect
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.compose.stringRes

@Composable
fun SmartbarScreen() = FlorisScreen {
    title = stringRes(R.string.settings__smartbar__title)
    previewFieldVisible = true

    val navController = LocalNavController.current

    content {
        SwitchPreference(
            prefs.smartbar.enabled,
            title = stringRes(R.string.pref__smartbar__enabled__label),
            summary = stringRes(R.string.pref__smartbar__enabled__summary),
        )
        run {
            val logoPref = prefs.smartbar.uiShowLogo
            val langPref = prefs.smartbar.uiShowLang
            val slot1Pref = prefs.smartbar.uiShowSlot1
            val slot2Pref = prefs.smartbar.uiShowSlot2
            val hidePref = prefs.smartbar.uiShowHide
            val showLogo by logoPref.observeAsState()
            val showLang by langPref.observeAsState()
            val showSlot1 by slot1Pref.observeAsState()
            val showSlot2 by slot2Pref.observeAsState()
            val showHide by hidePref.observeAsState()
            var showShortcutsDialog by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            Preference(
                title = stringRes(R.string.ui__shortcuts__title),
                summary = stringRes(R.string.ui__shortcuts__summary),
                onClick = { showShortcutsDialog = true },
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
            )
            if (showShortcutsDialog) {
                @Composable
                fun Item(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = onChange)
                        Text(label)
                    }
                }
                AlertDialog(
                    onDismissRequest = { showShortcutsDialog = false },
                    title = { Text(stringRes(R.string.ui__shortcuts__title)) },
                    text = {
                        Column {
                            Item(stringRes(R.string.ui__shortcuts__logo), showLogo) { v -> scope.launch { logoPref.set(v) } }
                            Item(stringRes(R.string.ui__shortcuts__language), showLang) { v -> scope.launch { langPref.set(v) } }
                            Item(stringRes(R.string.ui__shortcuts__button1), showSlot1) { v -> scope.launch { slot1Pref.set(v) } }
                            Item(stringRes(R.string.ui__shortcuts__button2), showSlot2) { v -> scope.launch { slot2Pref.set(v) } }
                            Item(stringRes(R.string.ui__shortcuts__hide), showHide) { v -> scope.launch { hidePref.set(v) } }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showShortcutsDialog = false }) {
                            Text(stringRes(R.string.ui__hint_size__done))
                        }
                    },
                )
            }
        }

        DialogSliderPreference(
            prefs.smartbar.heightFactor,
            title = stringRes(R.string.pref__smartbar__height_factor__label),
            valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
            min = 70,
            max = 160,
            stepIncrement = 5,
            enabledIf = { prefs.smartbar.enabled isEqualTo true },
        )
        DialogSliderPreference(
            prefs.smartbar.actionIconScale,
            title = stringRes(R.string.pref__smartbar__action_icon_scale__label),
            valueLabel = { stringRes(R.string.unit__percent__symbol, "v" to it) },
            min = 60,
            max = 220,
            stepIncrement = 5,
            enabledIf = { prefs.smartbar.enabled isEqualTo true },
        )
        Preference(
            title = stringRes(R.string.settings__toolbar_buttons__title),
            summary = stringRes(R.string.settings__toolbar_buttons__summary),
            onClick = { navController.navigate(Routes.Settings.ToolbarButtons) },
        )

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__group_layout_specific__label)) {
            ListPreference(
                prefs.suggestion.displayMode,
                title = stringRes(R.string.pref__suggestion__display_mode__label),
                entries = enumDisplayEntriesOf(CandidatesDisplayMode::class),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
                visibleIf = { prefs.smartbar.layout isNotEqualTo SmartbarLayout.ACTIONS_ONLY },
            )
            SwitchPreference(
                prefs.smartbar.flipToggles,
                title = stringRes(R.string.pref__smartbar__flip_toggles__label),
                summary = stringRes(R.string.pref__smartbar__flip_toggles__summary),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
                visibleIf = {
                    prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED ||
                        prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED
                },
            )
            // TODO: schedule to remove this preference in the future, but keep it for now so users
            //  know why the setting is not available anymore. Also force enable it for UI display.
            SideEffect {
                // prefs.smartbar.sharedActionsAutoExpandCollapse.set(true)
            }
            SwitchPreference(
                prefs.smartbar.sharedActionsAutoExpandCollapse,
                title = stringRes(R.string.pref__smartbar__shared_actions_auto_expand_collapse__label),
                summary = "[Since v0.4.1] Always enabled due to UX issues",
                enabledIf = { false },
                visibleIf = { prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED },
            )
            ListPreference(
                listPref = prefs.smartbar.extendedActionsPlacement,
                title = stringRes(R.string.pref__smartbar__extended_actions_placement__label),
                entries = enumDisplayEntriesOf(ExtendedActionsPlacement::class),
                enabledIf = { prefs.smartbar.enabled isEqualTo true },
                visibleIf = { prefs.smartbar.layout isEqualTo SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED },
            )
        }
    }
}
