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

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.jetpref.datastore.model.observeAsState

/**
 * Returns the string in the language of the app (the "settings language"), not in the language that is currently
 * being typed.
 */
@Composable
fun uiString(@StringRes id: Int): String {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val tag by prefs.other.settingsLanguage.observeAsState()
    val localized = remember(tag, context) {
        val config = Configuration(context.resources.configuration)
        val locale = if (tag == "auto") FlorisLocale.default() else FlorisLocale.fromTag(tag)
        config.setLocale(locale.base)
        context.createConfigurationContext(config)
    }
    return localized.resources.getString(id)
}
