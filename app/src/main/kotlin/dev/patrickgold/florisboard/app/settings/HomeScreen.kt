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

package dev.patrickgold.florisboard.app.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.Preference
import org.florisboard.lib.compose.FlorisErrorCard
import org.florisboard.lib.compose.FlorisWarningCard
import org.florisboard.lib.compose.stringRes

@Composable
fun HomeScreen() = FlorisScreen {
    // The welcome text is shown in a card inside the page instead of the top bar
    val welcome = stringRes(R.string.settings__home__title)
    title = ""
    navigationIconVisible = false
    previewFieldVisible = true

    val navController = LocalNavController.current
    val context = LocalContext.current

    content {
        val isCollapsed by prefs.internal.homeIsBetaToolboxCollapsed.observeAsState()

        val isFlorisBoardEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
        val isFlorisBoardSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)
        if (!isFlorisBoardEnabled) {
            FlorisErrorCard(
                modifier = Modifier.padding(8.dp),
                showIcon = false,
                text = stringRes(R.string.settings__home__ime_not_enabled),
                onClick = { InputMethodUtils.showImeEnablerActivity(context) },
            )
        } else if (!isFlorisBoardSelected) {
            FlorisWarningCard(
                modifier = Modifier.padding(8.dp),
                showIcon = false,
                text = stringRes(R.string.settings__home__ime_not_selected),
                onClick = { InputMethodUtils.showImePicker(context) },
            )
        }

        /*Card(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Welcome to the 0.4 alpha series!",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    IconButton(onClick = { this@content.prefs.internal.homeIsBetaToolboxCollapsed.set(!isCollapsed) }) {
                        Icon(
                            painter = painterResource(if (isCollapsed) {
                                R.drawable.ic_keyboard_arrow_down
                            } else {
                                R.drawable.ic_keyboard_arrow_up
                            }),
                            contentDescription = null,
                        )
                    }
                }
                if (!isCollapsed) {
                    Text("0.4 will be quite a big release and finally work on adding support for word suggestion and inline autocorrect within the keyboard UI, at first for Latin-based languages. Additionally general improvements and bug fixes will also be made.\n")
                    Text("Currently the alpha releases are preparations for the suggestions implementation and general improvements and bug fixes.\n")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Note that this release does not contain support for word suggestions (will show the current word plus numbers as a placeholder).", color = Color.Red)
                    Text("Please DO NOT file an issue for this. It is already more than known and a major goal for implementation in 0.4.0. Thank you!\n")
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }*/
        run {
            val cut = welcome.lastIndexOf(' ').coerceAtLeast(0)
            HomeCard(fillWidth = 0.56f, topSpace = 28.dp, scaleText = false) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    val style = MaterialTheme.typography.titleLarge
                    androidx.compose.material3.Text(welcome.substring(0, cut).trim(), style = style)
                    androidx.compose.material3.Text(welcome.substring(cut).trim(), style = style)
                }
            }
        }
        HomeCard { Preference(
            icon = Icons.Default.Language,
            title = stringRes(R.string.settings__localization__title),
            onClick = { navController.navigate(Routes.Settings.Localization) },
        ) }
        HomeCard { Preference(
            icon = Icons.Outlined.Palette,
            title = stringRes(R.string.settings__theme__title),
            onClick = { navController.navigate(Routes.Settings.Theme) },
        ) }
        HomeCard { Preference(
            icon = Icons.Outlined.Keyboard,
            title = stringRes(R.string.settings__keyboard__title),
            onClick = { navController.navigate(Routes.Settings.Keyboard) },
        ) }
        HomeCard { Preference(
            icon = Icons.Default.SmartButton,
            title = stringRes(R.string.settings__smartbar__title),
            onClick = { navController.navigate(Routes.Settings.Smartbar) },
        ) }
        HomeCard { Preference(
            icon = Icons.Default.Spellcheck,
            title = stringRes(R.string.settings__typing__title),
            onClick = { navController.navigate(Routes.Settings.Typing) },
        ) }
        HomeCard { Preference(
            icon = Icons.Default.Gesture,
            title = stringRes(R.string.settings__gestures__title),
            onClick = { navController.navigate(Routes.Settings.Gestures) },
        ) }
        HomeCard { Preference(
            icon = Icons.AutoMirrored.Outlined.Assignment,
            title = stringRes(R.string.settings__clipboard__title),
            onClick = { navController.navigate(Routes.Settings.Clipboard) },
        ) }
        HomeCard { Preference(
            icon = Icons.Default.SentimentSatisfiedAlt,
            title = stringRes(R.string.settings__media__title),
            onClick = { navController.navigate(Routes.Settings.Media) },
        ) }
        HomeCard { Preference(
            icon = Icons.Default.Extension,
            title = stringRes(R.string.ext__home__title),
            onClick = { navController.navigate(Routes.Ext.Home) },
        ) }
        HomeCard { Preference(
            icon = Icons.Outlined.Build,
            title = stringRes(R.string.settings__other__title),
            onClick = { navController.navigate(Routes.Settings.Other) },
        ) }
        HomeCard { Preference(
            icon = Icons.Outlined.Info,
            title = stringRes(R.string.about__title),
            onClick = { navController.navigate(Routes.Settings.About) },
        ) }
    }
}

/** A main-menu entry: pill-shaped (half-circle ends) light card with 35% larger text. */
@androidx.compose.runtime.Composable
private fun HomeCard(
    fillWidth: Float = 1f,
    topSpace: androidx.compose.ui.unit.Dp = 0.dp,
    scaleText: Boolean = true,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dark = androidx.compose.ui.graphics.Color(0xFF1B1B1B)
    val scheme = MaterialTheme.colorScheme.copy(onSurface = dark, onSurfaceVariant = dark, onBackground = dark)
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density = density.density,
            fontScale = if (scaleText) density.fontScale * 1.35f else density.fontScale,
        ),
        androidx.compose.material3.LocalContentColor provides dark,
    ) {
        MaterialTheme(colorScheme = scheme) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().padding(top = topSpace),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth(fillWidth)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    // #FFC0FFFC made 40% lighter
                    color = androidx.compose.ui.graphics.Color(0xFFD9FFFD),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF9EDFDA)),
                ) {
                    content()
                }
            }
        }
    }
}
