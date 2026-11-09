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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import androidx.compose.foundation.layout.Box
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.editorInstance
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.runtime.rememberCoroutineScope
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.subtypeManager
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.snyggBackdrop
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

private val MenuHighlightColor = Color(0xFF2ECC71)

/** Size of the toolbar icons. */
private val MenuShape = RoundedCornerShape(6.dp)
private val MenuIconBoxSize = 40.dp
private val ToolbarIconSize = 28.dp

/** The edit-text icon keeps its larger size. */
private val EditTextIconSize = 32.dp

/**
 * Gesture for toolbar buttons with a popup menu: a tap runs [onTap], a long press opens the popup menu with the entries
 * returned by [entries]; the finger then selects an entry and releasing it runs the entry.
 */
private fun Modifier.uiMenuGesture(
    key: Any?,
    onTap: () -> Unit,
    entries: () -> List<UiMenuEntry>,
    iconAnchor: (() -> Pair<Int, Int>)? = null,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id)
        if (longPress == null) {
            // Released (or moved) before the long press time: counts as a tap.
            onTap()
            return@awaitEachGesture
        }
        UiPanelState.menuEntries = entries()
        UiPanelState.menuIconOnly = iconAnchor != null
        iconAnchor?.invoke()?.let { (left, width) ->
            UiPanelState.menuAnchorLeftPx = left
            UiPanelState.menuAnchorWidthPx = width
        }
        UiPanelState.menuScroll = 0
        UiPanelState.menuFingerSlot = -1
        UiPanelState.languageMenuOpen = true
        val startY = longPress.position.y
        var moved = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val slot = UiPanelState.menuItemHeightPx
            val y = change.position.y
            if (!moved && kotlin.math.abs(y - startY) > slot / 3f) moved = true
            // Language menu: item 0 starts one slot above the toolbar, item 1 is level with the toolbar, and so on.
            // Icon menu: item 0 is level with the toolbar (it replaces the pressed button), the rest goes downwards.
            // The touch position is relative to the button, which has a 4dp padding inside the toolbar.
            val index = if (iconAnchor != null) {
                kotlin.math.floor((y + 4.dp.toPx()) / slot).toInt()
            } else {
                kotlin.math.floor((y + slot) / slot).toInt()
            }
            UiPanelState.menuFingerSlot = if (moved && index in 0 until UiPanelState.MENU_VISIBLE) index else -1
            val released = !change.pressed
            change.consume()
            if (released) break
        }
        val chosen = UiPanelState.menuEntries.getOrNull(UiPanelState.languageMenuHighlight)
        UiPanelState.languageMenuOpen = false
        UiPanelState.menuFingerSlot = -1
        chosen?.action?.invoke()
    }
}

/**
 * The toolbar of the "UI keyboard", shown while there are no word suggestions.
 *
 * Left to right: logo (opens the settings), edit-text panel, language box, emoji, hide keyboard.
 */
@Composable
fun UiToolbar(modifier: Modifier = Modifier, middle: (@Composable BoxScope.() -> Unit)? = null) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    val dispatcher = keyboardManager.inputEventDispatcher
    val barHeight = FlorisImeSizing.smartbarHeight

    fun languageName(locale: java.util.Locale): String {
        val name = locale.getDisplayLanguage(locale)
        return name.replaceFirstChar { it.uppercase() }
    }

    val labelNumeric = uiString(R.string.ui__menu__numeric)
    val labelSymbols = uiString(R.string.ui__menu__symbols)
    val labelThemes = uiString(R.string.ui__menu__themes)
    val labelSettings = uiString(R.string.ui__menu__settings)

    fun buildEntries(): List<UiMenuEntry> = buildList {
        for (subtype in subtypes) {
            add(UiMenuEntry(languageName(subtype.primaryLocale.base), Icons.Default.Language) {
                subtypeManager.switchToSubtypeById(subtype.id)
            })
        }
        add(UiMenuEntry(labelNumeric, Icons.Default.Dialpad) { UiPanelState.panel = UiPanel.NUMERIC })
        add(UiMenuEntry(labelSymbols, Icons.Default.Tag) {
            UiPanelState.panel = UiPanel.NONE
            dispatcher.sendDownUp(TextKeyData.VIEW_SYMBOLS)
        })
        add(UiMenuEntry(labelThemes, Icons.Default.ColorLens) { dispatcher.sendDownUp(TextKeyData.THEMES) })
        add(UiMenuEntry(labelSettings, dev.patrickgold.florisboard.ime.uikb.UiIcons.Gear) { dispatcher.sendDownUp(TextKeyData.SETTINGS) })
    }

    val outlineColor = rememberSnyggThemeQuery(FlorisImeUi.SmartbarActionKey.elementName)
        .foreground(Color.Gray).copy(alpha = 0.5f)
    val lineWidth = 1.dp
    val langOutline = Modifier.border(lineWidth, outlineColor, MenuShape)

    @Composable
    fun Divider() {
        Box(
            modifier = Modifier
                .width(lineWidth)
                .height(barHeight * 0.6f)
                .background(outlineColor),
        )
    }

    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val slotEdit by prefs.smartbar.uiSlotEdit.observeAsState()
    val slotEmoji by prefs.smartbar.uiSlotEmoji.observeAsState()
    // "Toolbar icon size" setting: 100 % is the normal size. The icon never gets bigger than the bar.
    val iconScalePercent by prefs.smartbar.actionIconScale.observeAsState()
    fun iconSize(base: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
        val scaled = base * (iconScalePercent / 100f)
        return scaled.coerceAtMost(maxOf(barHeight - 8.dp, 12.dp)).coerceAtLeast(10.dp)
    }

    fun runFeature(feature: UiFeature) {
        when (feature) {
            UiFeature.EMOJI -> {
                UiPanelState.panel = UiPanel.NONE
                dispatcher.sendDownUp(TextKeyData.IME_UI_MODE_MEDIA)
            }
            UiFeature.EDIT_TEXT -> UiPanelState.toggle(UiPanel.EDIT_TEXT)
            UiFeature.CLIPBOARD -> {
                UiPanelState.panel = UiPanel.NONE
                dispatcher.sendDownUp(TextKeyData.IME_UI_MODE_CLIPBOARD)
            }
            UiFeature.VOICE -> dispatcher.sendDownUp(TextKeyData.VOICE_INPUT)
        }
    }

    @Composable
    fun featureLabels() = UiFeature.entries.map { uiString(it.labelRes) }
    val featureLabels = featureLabels()

    /** A customizable button: tap runs its feature, long press opens a menu to pick another feature. */
    @Composable
    fun SlotButton(feature: UiFeature, pref: dev.patrickgold.jetpref.datastore.model.PreferenceData<UiFeature>) {
        var anchorLeft by remember { mutableIntStateOf(0) }
        var anchorWidth by remember { mutableIntStateOf(0) }
        SnyggBox(
            elementName = FlorisImeUi.SmartbarActionKey.elementName,
            modifier = Modifier
                .height(barHeight)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .aspectRatio(1f)
                .onGloballyPositioned { coords ->
                    anchorLeft = coords.positionInRoot().x.toInt()
                    anchorWidth = coords.size.width
                },
            clickAndSemanticsModifier = Modifier.uiMenuGesture(
                key = feature,
                onTap = { runFeature(feature) },
                entries = {
                    UiFeature.entries.mapIndexed { i, f ->
                        UiMenuEntry(featureLabels[i], f.icon, f.iconRes) { scope.launch { pref.set(f) } }
                    }
                },
                iconAnchor = { anchorLeft to anchorWidth },
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (feature.iconRes != null) {
                SnyggIcon(
                    modifier = Modifier.size(iconSize(ToolbarIconSize)),
                    painter = painterResource(feature.iconRes),
                )
            } else {
                SnyggIcon(
                    modifier = Modifier.size(iconSize(ToolbarIconSize)),
                    imageVector = feature.icon!!,
                )
            }
        }
    }

    @Composable
    fun BarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
        SnyggIconButton(
            elementName = FlorisImeUi.SmartbarActionKey.elementName,
            onClick = onClick,
            modifier = Modifier.height(barHeight).padding(horizontal = 4.dp, vertical = 4.dp).aspectRatio(1f),
        ) {
            content()
        }
    }

    @Composable
    fun RowScope.Cell(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }

    // Layout: [ logo ] | [ edit  language box (140dp)  emoji ] | [ hide ]
    // Logo and hide always stay. The area between the two dividers is the "middle" and can be replaced by [middle]
    // (the recently copied text or the word suggestions).
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(barHeight)) {
        val sideCell = (maxWidth - 140.dp) / 4
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(sideCell).fillMaxHeight(), contentAlignment = Alignment.Center) {
                BarButton(onClick = { UiPanelState.toggle(UiPanel.ACTIONS) }) {
                    SnyggIcon(modifier = Modifier.size(iconSize(ToolbarIconSize)), painter = painterResource(R.drawable.ui_toolbar_logo))
                }
            }
            Divider()
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                if (middle != null) {
                    middle()
                } else {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            SlotButton(slotEdit, prefs.smartbar.uiSlotEdit)
                        }
                        // Language box: tap switches to the next language, long press opens the popup menu.
                        SnyggBox(
                            elementName = FlorisImeUi.SmartbarActionKey.elementName,
                            selector = if (UiPanelState.languageMenuOpen) SnyggSelector.PRESSED else SnyggSelector.NONE,
                            modifier = Modifier.width(140.dp).height(barHeight).padding(vertical = 6.dp).then(langOutline),
                            clickAndSemanticsModifier = Modifier.uiMenuGesture(
                                key = subtypes,
                                onTap = {
                                    // Stay on the current panel (numeric, symbols...) when the language changes
                                    subtypeManager.switchToNextSubtype()
                                },
                                entries = { buildEntries() },
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SnyggText(text = languageName(activeSubtype.primaryLocale.base))
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            SlotButton(slotEmoji, prefs.smartbar.uiSlotEmoji)
                        }
                    }
                }
            }
            Divider()
            Box(modifier = Modifier.width(sideCell).fillMaxHeight(), contentAlignment = Alignment.Center) {
                BarButton(onClick = { dispatcher.sendDownUp(TextKeyData.IME_HIDE_UI) }) {
                    SnyggIcon(modifier = Modifier.size(iconSize(ToolbarIconSize)), painter = painterResource(R.drawable.ui_icon_hide))
                }
            }
        }
    }
}

/**
 * The "recently copied text" chip. Fills the middle area of the toolbar, has the key color of the active theme,
 * and pastes the text on tap.
 */
@Composable
fun UiClipboardChip(item: ClipboardItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val editorInstance by context.editorInstance()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val prefs by FlorisPreferenceStore
    val keyStyle = rememberSnyggThemeQuery(FlorisImeUi.Key.elementName)
    val chipColor = keyStyle.background(Color.Gray)
    val textColor = keyStyle.foreground(Color.White)
    val label = if (item.isSensitive) "••••••" else (item.text ?: "").replace('\n', ' ').trim()
    // The icon sits on the right of Arabic (right-to-left) text and on the left of any other text; a long text is cut
    // at its end only, so the beginning of the sentence stays visible.
    val isRtlText = remember(label) {
        label.isNotEmpty() &&
            android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(label, 0, label.length)
    }
    val isPhone = remember(label, item.isSensitive) {
        !item.isSensitive && dev.patrickgold.florisboard.lib.util.NetworkUtils.isPhoneNumber(label)
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            if (isRtlText) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
    ) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(chipColor, RoundedCornerShape(50))
            .pointerInput(item) {
                // Tap: paste the text. Holding for the chosen time (default 3 seconds): dismiss the chip without pasting.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    // true: finger lifted (tap), false: gesture cancelled (finger moved away), null: the hold time passed
                    val holdMillis = prefs.clipboard.recentChipHoldSeconds.get().coerceIn(1, 10) * 1000L
                    val result = withTimeoutOrNull(holdMillis) {
                        waitForUpOrCancellation() != null
                    }
                    when (result) {
                        true -> {
                            editorInstance.commitClipboardItem(item)
                            UiPanelState.lastUsedClipTimestamp = item.creationTimestampMs
                            UiPanelState.lastUsedClipText = item.text
                        }
                        false -> {}
                        null -> {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            UiPanelState.lastUsedClipTimestamp = item.creationTimestampMs
                            UiPanelState.lastUsedClipText = item.text
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Icon and text form one group, centred in the chip. The icon sits next to the start of the text (left of
        // Latin text, right of Arabic text); a long text is cut at its end only.
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                modifier = Modifier.size(20.dp),
                imageVector = if (isPhone) Icons.Default.Phone else Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = null,
                tint = textColor,
            )
            Spacer(Modifier.width(6.dp))
            androidx.compose.material3.Text(
                modifier = Modifier.weight(1f, fill = false),
                text = label,
                color = textColor,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
    }
}

/**
 * The popup menu of the language box. It is drawn on the left side, starting one item above the toolbar, so the items
 * are split between the area above and below the toolbar. Only [UiPanelState.MENU_VISIBLE] entries are visible at
 * once: the two middle ones normally, the first and last faded. Holding the finger on a faded entry scrolls the list.
 * Must be placed at the top-start corner of the keyboard view.
 */
@Composable
fun UiLanguageMenu(modifier: Modifier = Modifier) {
    val entries = UiPanelState.menuEntries
    if (!UiPanelState.languageMenuOpen || entries.isEmpty()) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bar = FlorisImeSizing.smartbarHeight
    val ui = FlorisImeSizing.keyboardUiHeight()
    val keyColor = rememberSnyggThemeQuery(FlorisImeUi.Key.elementName).background(Color.Gray)
    val outlineColor = rememberSnyggThemeQuery(FlorisImeUi.SmartbarActionKey.elementName)
        .foreground(Color.Gray).copy(alpha = 0.5f)
    val visible = minOf(UiPanelState.MENU_VISIBLE, entries.size)
    val maxScroll = entries.size - visible
    val slot = minOf(bar, (bar + ui) / maxOf(visible - 1, 1))
    SideEffect { UiPanelState.menuItemHeightPx = with(density) { slot.roundToPx() }.coerceAtLeast(1) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(350)
            if (UiPanelState.menuFingerSlot == 0 && UiPanelState.menuScroll > 0) {
                UiPanelState.menuScroll -= 1
            } else if (UiPanelState.menuFingerSlot == visible - 1 && UiPanelState.menuScroll < maxScroll) {
                UiPanelState.menuScroll += 1
            }
        }
    }
    val iconOnly = UiPanelState.menuIconOnly
    val menuModifier = if (iconOnly) {
        modifier
            .absoluteOffset(
                x = with(density) { (UiPanelState.menuAnchorLeftPx + UiPanelState.menuAnchorWidthPx / 2).toDp() } -
                    MenuIconBoxSize / 2,
            )
            .width(MenuIconBoxSize)
    } else {
        modifier.absoluteOffset(y = -slot).width(190.dp)
    }
    val textColor = rememberSnyggThemeQuery(FlorisImeUi.Key.elementName).foreground(Color.White)
    Column(modifier = menuModifier) {
        for (slotIndex in 0 until visible) {
            val index = UiPanelState.menuScroll + slotIndex
            val entry = entries.getOrNull(index) ?: continue
            val highlighted = index == UiPanelState.languageMenuHighlight
            // Both menus: the selected item is 10% bigger and the neighbours move away so the gaps stay equal. The
            // scale and the shift are animated, only the drawing moves (the touch slots stay where they are).
            val sel = UiPanelState.languageMenuHighlight
            val scaleAnim by animateFloatAsState(
                targetValue = if (highlighted) 1.1f else 1f,
                animationSpec = tween(durationMillis = 120),
                label = "menuItemScale",
            )
            val shiftAnim by animateFloatAsState(
                targetValue = when {
                    sel < 0 || index == sel -> 0f
                    index < sel -> -1f
                    else -> 1f
                },
                animationSpec = tween(durationMillis = 120),
                label = "menuItemShift",
            )
            if (iconOnly && (entry.icon != null || entry.iconRes != null)) {
                // Icon menu: the key background of the theme, the selected icon turns green.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slot)
                        .padding(vertical = maxOf(0.dp, (slot - MenuIconBoxSize) / 2))
                        .graphicsLayer {
                            scaleX = scaleAnim
                            scaleY = scaleAnim
                            translationY = shiftAnim * 0.05f * size.height
                        }
                        .snyggBackdrop(FlorisImeUi.Key.elementName, MenuShape)
                        .background(keyColor, MenuShape)
                        .border(1.5.dp, outlineColor, MenuShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val iconModifier = Modifier.size(
                        if (entry.icon === UiIcons.EditText) EditTextIconSize else ToolbarIconSize,
                    )
                    if (highlighted) {
                        // Soft glow around the selected icon, in the icon's own (green) color
                        val glowModifier = iconModifier.drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(MenuHighlightColor.copy(alpha = 0.30f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 0.95f,
                                ),
                                radius = size.minDimension * 0.95f,
                            )
                        }
                        if (entry.iconRes != null) {
                            androidx.compose.material3.Icon(
                                modifier = glowModifier,
                                painter = painterResource(entry.iconRes),
                                contentDescription = null,
                                tint = MenuHighlightColor,
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                modifier = glowModifier,
                                imageVector = entry.icon!!,
                                contentDescription = null,
                                tint = MenuHighlightColor,
                            )
                        }
                    } else if (entry.iconRes != null) {
                        SnyggIcon(modifier = iconModifier, painter = painterResource(entry.iconRes))
                    } else {
                        SnyggIcon(modifier = iconModifier, imageVector = entry.icon!!)
                    }
                }
            } else {
                // Language menu: the selected item is 10% bigger (box and text), green, and its text glows in the
                // text color. The neighbours move away so the gaps stay equal.
                // The selected item grows by 5% of its height on both sides; the items before and after it move away
                // by the same amount so that all gaps stay equal. Only the drawing moves (animated), the touch slots
                // stay where they are.
                val bgAnim by animateColorAsState(
                    targetValue = if (highlighted) MenuHighlightColor else keyColor,
                    animationSpec = tween(durationMillis = 120),
                    label = "menuItemColor",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slot)
                        .padding(vertical = 3.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim
                            scaleY = scaleAnim
                            translationY = shiftAnim * 0.05f * size.height
                        }
                        .snyggBackdrop(FlorisImeUi.Key.elementName, MenuShape)
                        .background(bgAnim, MenuShape)
                        .border(1.5.dp, outlineColor, MenuShape),
                    contentAlignment = Alignment.Center,
                ) {
                    // The icons (if any) form one column at the right edge, so they all line up; the text stays
                    // centered in the item.
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (entry.icon != null) {
                                val iconModifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 32.dp)
                                    .size(22.dp)
                                if (highlighted) {
                                    androidx.compose.material3.Icon(
                                        modifier = iconModifier,
                                        imageVector = entry.icon,
                                        contentDescription = null,
                                        tint = textColor,
                                    )
                                } else {
                                    SnyggIcon(modifier = iconModifier, imageVector = entry.icon)
                                }
                            }
                            Box(modifier = Modifier.align(Alignment.Center)) {
                                if (highlighted) {
                                    androidx.compose.material3.Text(
                                        text = entry.label,
                                        color = textColor,
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = textColor,
                                                offset = androidx.compose.ui.geometry.Offset.Zero,
                                                blurRadius = 14f,
                                            ),
                                        ),
                                    )
                                } else {
                                    SnyggText(text = entry.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
