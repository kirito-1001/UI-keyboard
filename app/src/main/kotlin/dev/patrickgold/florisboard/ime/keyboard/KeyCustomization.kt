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

package dev.patrickgold.florisboard.ime.keyboard

import dev.patrickgold.florisboard.lib.io.DefaultJsonConfig
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Normalizes a key code so that upper and lower case variants of a character key share the same customization.
 */
fun normalizeKeyCode(code: Int): Int {
    return if (code > 0) Character.toLowerCase(code) else code
}

/**
 * A user customization of a single key, identified by its (normalized) key code.
 *
 * @property code The normalized key code this customization applies to.
 * @property hidden If true the key is removed from the keyboard.
 * @property moveBy Number of positions the key is moved inside its row. Negative moves left, positive moves right.
 * @property background Custom background color as ARGB int, or null to use the theme color.
 * @property foreground Custom label/icon color as ARGB int, or null to use the theme color.
 */
@Serializable
data class KeyCustomization(
    val code: Int,
    val hidden: Boolean = false,
    val moveBy: Int = 0,
    val background: Int? = null,
    val foreground: Int? = null,
) {
    val isDefault: Boolean
        get() = !hidden && moveBy == 0 && background == null && foreground == null
}

@Serializable
data class KeyCustomizations(
    val items: List<KeyCustomization> = emptyList(),
) {
    fun find(code: Int): KeyCustomization? {
        val normalized = normalizeKeyCode(code)
        return items.firstOrNull { it.code == normalized }
    }

    fun isHidden(code: Int): Boolean = find(code)?.hidden == true

    /**
     * Returns a copy where the customization for [code] is replaced by the result of [transform]. Customizations
     * which equal the defaults are dropped to keep the stored list small.
     */
    fun update(code: Int, transform: (KeyCustomization) -> KeyCustomization): KeyCustomizations {
        val normalized = normalizeKeyCode(code)
        val current = find(normalized) ?: KeyCustomization(normalized)
        val updated = transform(current).copy(code = normalized)
        val others = items.filter { it.code != normalized }
        return KeyCustomizations(if (updated.isDefault) others else others + updated)
    }

    companion object {
        private val JsonConfig = Json(DefaultJsonConfig) {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = false
        }
    }

    object Serializer : PreferenceSerializer<KeyCustomizations> {
        override fun serialize(value: KeyCustomizations): String {
            return JsonConfig.encodeToString(value)
        }

        override fun deserialize(value: String): KeyCustomizations {
            return JsonConfig.decodeFromString(value)
        }
    }
}
