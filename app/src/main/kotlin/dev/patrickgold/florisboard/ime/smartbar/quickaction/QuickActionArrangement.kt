/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.lib.io.DefaultJsonConfig
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

val QuickActionJsonConfig = Json(DefaultJsonConfig) {
    classDiscriminator = "$"
    encodeDefaults = false
    ignoreUnknownKeys = true
    isLenient = false

    serializersModule += SerializersModule {
        polymorphic(QuickAction::class) {
            subclass(QuickAction.InsertKey::class, QuickAction.InsertKey.serializer())
            subclass(QuickAction.InsertText::class, QuickAction.InsertText.serializer())
            defaultDeserializer { QuickAction.InsertKey.serializer() }
        }
    }
}

@Serializable
data class QuickActionArrangement(
    val stickyAction: QuickAction?,
    val dynamicActions: List<QuickAction>,
    val hiddenActions: List<QuickAction>,
) {
    operator fun contains(action: QuickAction): Boolean {
        return stickyAction == action || dynamicActions.contains(action) || hiddenActions.contains(action)
    }

    /**
     * Adds the "Themes" action (right after "Settings") for users whose saved arrangement was created before this
     * action existed. An action the user hid stays hidden, because hidden actions count as present.
     */
    fun withThemesAction(): QuickActionArrangement {
        val themes = QuickAction.InsertKey(TextKeyData.THEMES)
        if (themes in this) return this
        val settingsIndex = dynamicActions.indexOf(QuickAction.InsertKey(TextKeyData.SETTINGS))
        val newDynamic = dynamicActions.toMutableList().apply {
            if (settingsIndex >= 0) add(settingsIndex + 1, themes) else add(themes)
        }
        return copy(dynamicActions = newDynamic)
    }

    /**
     * Makes "Voice input" a normal toolbar action (shown in the logo grid, and movable / hideable in
     * Settings > Smartbar > Toolbar buttons). Before, it was the fixed "sticky" action, which is in neither.
     * An action the user hid stays hidden, because hidden actions count as present.
     */
    fun withVoiceAction(): QuickActionArrangement {
        val voice = QuickAction.InsertKey(TextKeyData.VOICE_INPUT)
        if (stickyAction == voice) {
            return copy(
                stickyAction = null,
                dynamicActions = listOf<QuickAction>(voice) + dynamicActions.filter { it != voice },
            )
        }
        if (voice in this) return this
        return copy(dynamicActions = listOf<QuickAction>(voice) + dynamicActions)
    }

    fun distinct(): QuickActionArrangement {
        val distinctSet = mutableSetOf<QuickAction>()
        if (stickyAction != null) {
            distinctSet.add(stickyAction)
        }
        val distinctDynamicActions = dynamicActions.filter { distinctSet.add(it) }
        val distinctHiddenActions = hiddenActions.filter { distinctSet.add(it) }
        return QuickActionArrangement(
            stickyAction = stickyAction,
            dynamicActions = distinctDynamicActions,
            hiddenActions = distinctHiddenActions,
        )
    }

    companion object {
        val Default = QuickActionArrangement(
            stickyAction = null,
            dynamicActions = listOf(
                QuickAction.InsertKey(TextKeyData.VOICE_INPUT),
                QuickAction.InsertKey(TextKeyData.LANGUAGE_SWITCH),
                QuickAction.InsertKey(TextKeyData.UNDO),
                QuickAction.InsertKey(TextKeyData.REDO),
                QuickAction.InsertKey(TextKeyData.SETTINGS),
                QuickAction.InsertKey(TextKeyData.THEMES),
                QuickAction.InsertKey(TextKeyData.TOGGLE_INCOGNITO_MODE),
                QuickAction.InsertKey(TextKeyData.IME_UI_MODE_CLIPBOARD),
                QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA),
                QuickAction.InsertKey(TextKeyData.TOGGLE_COMPACT_LAYOUT),
                QuickAction.InsertKey(TextKeyData.TOGGLE_AUTOCORRECT),
                QuickAction.InsertKey(TextKeyData.ARROW_UP),
                QuickAction.InsertKey(TextKeyData.ARROW_DOWN),
                QuickAction.InsertKey(TextKeyData.ARROW_LEFT),
                QuickAction.InsertKey(TextKeyData.ARROW_RIGHT),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_CLEAR_PRIMARY_CLIP),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_COPY),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_CUT),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_PASTE),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
                QuickAction.InsertKey(TextKeyData.FORWARD_DELETE),
                QuickAction.InsertKey(TextKeyData.IME_HIDE_UI),
            ),
            hiddenActions = listOf(
            ),
        )
    }

    object Serializer : PreferenceSerializer<QuickActionArrangement> {
        override fun serialize(value: QuickActionArrangement): String {
            return QuickActionJsonConfig.encodeToString(value)
        }

        override fun deserialize(value: String): QuickActionArrangement {
            return QuickActionJsonConfig.decodeFromString(value)
        }
    }
}
