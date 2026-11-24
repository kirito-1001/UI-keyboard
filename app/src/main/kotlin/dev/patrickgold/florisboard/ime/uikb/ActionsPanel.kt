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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickAction
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionBarType
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionButton
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.snygg.ui.SnyggBox

/**
 * The panel opened by the logo button: it replaces the key area (the toolbar stays) and shows all toolbar actions
 * the user has enabled in Settings > Smartbar > Toolbar buttons (in that order) in a grid. The grid scrolls up and down, so actions added in the future always fit.
 */
@Composable
fun ActionsPanel(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val actionArrangement by prefs.smartbar.actionArrangement.observeAsState()
    val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()

    androidx.compose.runtime.LaunchedEffect(actionArrangement) {
        val updated = actionArrangement.withoutOneHandedActions().withThemesAction().withVoiceAction().withVaultAction()
        if (updated != actionArrangement) prefs.smartbar.actionArrangement.set(updated)
    }

    // Same list and order as set in Settings > Smartbar > Toolbar buttons; hidden actions are not shown
    val allActions = remember(actionArrangement) {
        actionArrangement.dynamicActions.filter { it !in actionArrangement.hiddenActions }.distinct()
    }

    // Drag and drop: long press a tile, drag it to a new place and release; the new order is saved to the same
    // arrangement that Settings > Smartbar > Toolbar buttons uses. Hidden actions keep their place.
    val order = remember(allActions) { mutableStateListOf<QuickAction>().apply { addAll(allActions) } }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val edgeZone = with(LocalDensity.current) { 56.dp.toPx() }
    var draggedKey by remember { mutableStateOf<Int?>(null) }
    var releasingKey by remember { mutableStateOf<Int?>(null) }
    val releaseAnim = remember { Animatable(0f) }
    var fingerPos by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }

    fun itemAt(p: Offset): LazyGridItemInfo? {
        return gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            p.x >= info.offset.x && p.x < info.offset.x + info.size.width &&
                p.y >= info.offset.y && p.y < info.offset.y + info.size.height
        }
    }

    fun moveTowards(p: Offset) {
        val key = draggedKey ?: return
        val from = order.indexOfFirst { it.hashCode() == key }
        if (from < 0) return
        // The grid has not been laid out with the latest order yet: wait for it, otherwise the tiles would flicker
        val draggedInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        if (draggedInfo.index != from) return
        val target = itemAt(p) ?: return
        val to = target.index
        if (to < 0 || to >= order.size || to == from) return
        order.add(to, order.removeAt(from))
    }

    fun saveOrder() {
        val hidden = actionArrangement.hiddenActions
        val iterator = order.toList().iterator()
        val newDynamic = actionArrangement.dynamicActions.distinct().map {
            if (it in hidden || !iterator.hasNext()) it else iterator.next()
        }
        if (newDynamic != actionArrangement.dynamicActions) {
            scope.launch { prefs.smartbar.actionArrangement.set(actionArrangement.copy(dynamicActions = newDynamic)) }
        }
    }

    // Scroll the grid while a tile is held near its top or bottom edge
    LaunchedEffect(draggedKey) {
        if (draggedKey == null) return@LaunchedEffect
        while (true) {
            val height = gridState.layoutInfo.viewportSize.height
            val y = fingerPos.y
            val speed = when {
                y < edgeZone -> -(edgeZone - y) / edgeZone * 18f
                y > height - edgeZone -> (y - (height - edgeZone)) / edgeZone * 18f
                else -> 0f
            }
            if (speed != 0f) {
                gridState.scrollBy(speed)
                moveTowards(fingerPos)
            }
            withFrameNanos { }
        }
    }

    SnyggBox(
        elementName = FlorisImeUi.SmartbarActionsOverflow.elementName,
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.keyboardUiHeight()),
    ) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(order) {
                    awaitEachGesture {
                        // Watched in the initial pass (before the tiles), so a long press can take the gesture over
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            var stop = false
                            while (!stop) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed ||
                                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                                ) {
                                    stop = true
                                }
                            }
                            true
                        }
                        if (cancelled != null || order.size < 2) return@awaitEachGesture
                        val info = itemAt(down.position) ?: return@awaitEachGesture
                        val key = info.key as? Int ?: return@awaitEachGesture
                        releasingKey = null
                        fingerPos = down.position
                        grabOffset = down.position - Offset(info.offset.x.toFloat(), info.offset.y.toFloat())
                        draggedKey = key
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                fingerPos = change.position
                                moveTowards(fingerPos)
                            }
                        } finally {
                            saveOrder()
                            releasingKey = key
                            draggedKey = null
                            scope.launch {
                                releaseAnim.snapTo(1f)
                                releaseAnim.animateTo(0f, tween(durationMillis = 160))
                                releasingKey = null
                            }
                        }
                    }
                },
            columns = GridCells.Fixed(4),
        ) {
            items(order, key = { it.hashCode() }) { action ->
                val k = action.hashCode()
                val isDragged = draggedKey == k
                val isReleasing = releasingKey == k
                QuickActionButton(
                    action = action,
                    evaluator = evaluator,
                    type = QuickActionBarType.INTERACTIVE_TILE,
                    modifier = if (isDragged || isReleasing) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                val factor = if (isDragged) 1f else releaseAnim.value
                                val itemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == k }
                                if (itemInfo != null) {
                                    translationX = (fingerPos.x - grabOffset.x - itemInfo.offset.x) * factor
                                    translationY = (fingerPos.y - grabOffset.y - itemInfo.offset.y) * factor
                                }
                                scaleX = 1f + 0.1f * factor
                                scaleY = 1f + 0.1f * factor
                                alpha = 1f - 0.05f * factor
                            }
                    } else {
                        Modifier.animateItem()
                    },
                )
            }
        }
    }
}
