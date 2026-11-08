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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun HomeScreen() {
    // The whole screen (also the top bar) gets the light page colour, so there is no differently coloured strip on top
    val base = MaterialTheme.colorScheme
    val dark = base.background.luminance() < 0.5f
    val page = if (dark) Color(0xFF121212) else Color(0xFFF0F0F0)
    MaterialTheme(
        colorScheme = base.copy(
            background = page, surface = page, surfaceContainer = page, surfaceContainerLow = page,
            surfaceContainerHigh = page, surfaceContainerHighest = page, surfaceContainerLowest = page,
        ),
    ) {
        HomeScreenContent()
    }
}

@Composable
private fun HomeScreenContent() = FlorisScreen {
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
        // Page: light background, the welcome text on top and the options in one rounded white sheet
        val darkPage = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val pageBg = if (darkPage) Color(0xFF121212) else Color(0xFFF0F0F0)
        val sheetBg = if (darkPage) Color(0xFF1E1E1E) else Color.White
        // The dividers between the options are faint: 25% opacity (faded by 50%, and then by 50% again)
        val lineColor = (if (darkPage) Color(0xFFDDDDDD) else Color(0xFF222222)).copy(alpha = 0.25f)
        val nameAt = welcome.indexOf("UI Keyboard").takeIf { it >= 0 }
        val first = if (nameAt != null) welcome.substring(0, nameAt).trim() else welcome
        Column(
            modifier = Modifier.fillMaxWidth().background(pageBg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Text(first, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (nameAt != null) {
                Text(
                    "UI Keyboard",
                    fontFamily = dev.patrickgold.florisboard.app.BudokanFont,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            run {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    HomeSheetItem(sheetBg, 0, 11) { HomePreference(
                    icon = Icons.Default.Language,
                    title = stringRes(R.string.settings__localization__title),
                    summary = stringRes(R.string.ui__home__summary__localization),
                    onClick = { navController.navigate(Routes.Settings.Localization) },
                    ) }
                    HomeSheetItem(sheetBg, 1, 11) { HomePreference(
                    icon = Icons.Outlined.Palette,
                    title = stringRes(R.string.settings__theme__title),
                    summary = stringRes(R.string.ui__home__summary__theme),
                    onClick = { navController.navigate(Routes.Settings.Theme) },
                    ) }
                    HomeSheetItem(sheetBg, 2, 11) { HomePreference(
                    icon = Icons.Outlined.Keyboard,
                    title = stringRes(R.string.settings__keyboard__title),
                    summary = stringRes(R.string.ui__home__summary__keyboard),
                    onClick = { navController.navigate(Routes.Settings.Keyboard) },
                    ) }
                    HomeSheetItem(sheetBg, 3, 11) { HomePreference(
                    icon = Icons.Default.SmartButton,
                    title = stringRes(R.string.settings__smartbar__title),
                    summary = stringRes(R.string.ui__home__summary__smartbar),
                    onClick = { navController.navigate(Routes.Settings.Smartbar) },
                    ) }
                    HomeSheetItem(sheetBg, 4, 11) { HomePreference(
                    icon = Icons.Default.Spellcheck,
                    title = stringRes(R.string.settings__typing__title),
                    summary = stringRes(R.string.ui__home__summary__typing),
                    onClick = { navController.navigate(Routes.Settings.Typing) },
                    ) }
                    HomeSheetItem(sheetBg, 5, 11) { HomePreference(
                    icon = Icons.Default.Gesture,
                    title = stringRes(R.string.settings__gestures__title),
                    summary = stringRes(R.string.ui__home__summary__gestures),
                    onClick = { navController.navigate(Routes.Settings.Gestures) },
                    ) }
                    HomeSheetItem(sheetBg, 6, 11) { HomePreference(
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    title = stringRes(R.string.settings__clipboard__title),
                    summary = stringRes(R.string.ui__home__summary__clipboard),
                    onClick = { navController.navigate(Routes.Settings.Clipboard) },
                    ) }
                    HomeSheetItem(sheetBg, 7, 11) { HomePreference(
                    icon = Icons.Default.SentimentSatisfiedAlt,
                    title = stringRes(R.string.settings__media__title),
                    summary = stringRes(R.string.ui__home__summary__media),
                    onClick = { navController.navigate(Routes.Settings.Media) },
                    ) }
                    HomeSheetItem(sheetBg, 8, 11) { HomePreference(
                    icon = Icons.Default.Extension,
                    title = stringRes(R.string.ext__home__title),
                    summary = stringRes(R.string.ui__home__summary__ext),
                    onClick = { navController.navigate(Routes.Ext.Home) },
                    ) }
                    HomeSheetItem(sheetBg, 9, 11) { HomePreference(
                    icon = Icons.Outlined.Build,
                    title = stringRes(R.string.settings__other__title),
                    summary = stringRes(R.string.ui__home__summary__other),
                    onClick = { navController.navigate(Routes.Settings.Other) },
                    ) }
                    HomeSheetItem(sheetBg, 10, 11) { HomePreference(
                    icon = Icons.Outlined.Info,
                    title = stringRes(R.string.about__title),
                    summary = stringRes(R.string.ui__home__summary__about),
                    onClick = { navController.navigate(Routes.Settings.About) },
                    ) }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/** One entry of the main menu: 35% larger text, drawn on the sheet colour. */
@androidx.compose.runtime.Composable
private fun HomeSheetItem(sheetBg: Color, index: Int, count: Int, content: @androidx.compose.runtime.Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    // The entries draw their own surface, so the sheet colour has to be part of the colour scheme too
    val scheme = MaterialTheme.colorScheme.copy(
        surface = sheetBg, background = sheetBg, surfaceVariant = sheetBg, surfaceContainer = sheetBg,
        surfaceContainerHigh = sheetBg, surfaceContainerHighest = sheetBg, surfaceContainerLow = sheetBg,
    )
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density = density.density,
            fontScale = density.fontScale * 1.35f,
        ),
    ) {
        val big = 23.dp
        val small = 7.dp
        val shape = RoundedCornerShape(
            topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
            bottomStart = if (index == count - 1) big else small, bottomEnd = if (index == count - 1) big else small,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = if (index == count - 1) 0.dp else 3.dp),
            shape = shape,
            color = sheetBg,
        ) {
            // The detail line under each title: 30% smaller and 50% fainter than the default
            val base = MaterialTheme.typography
            MaterialTheme(
                colorScheme = scheme,
                typography = base.copy(
                    bodyMedium = base.bodyMedium.copy(
                        fontSize = base.bodyMedium.fontSize * 0.7f,
                        lineHeight = base.bodyMedium.lineHeight * 0.7f,
                    ),
                ),
            ) { content() }
        }
    }
}

/** One row of the main menu: icon in the normal colour, the title, and a smaller, fainter detail line. */
@androidx.compose.runtime.Composable
private fun HomePreference(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
