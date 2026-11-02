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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.input.InputEventDispatcher
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.ProvideSnyggFontScale
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText

/** Opacity of keys which cannot do anything right now. */
private const val DisabledAlpha = 0.38f

private val ActiveDotColor = Color(0xFF2ECC71)

/**
 * A key which looks like a normal keyboard key (same theme element) and sends a [KeyData] through the
 * [InputEventDispatcher], so repeat-on-hold, feedback and the normal key handling work as for the letter keys.
 */
@Composable
fun UiDispatchKey(
    keyData: KeyData,
    inputEventDispatcher: InputEventDispatcher,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val feedback = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val prefs by FlorisPreferenceStore
    SnyggBox(
        elementName = FlorisImeUi.Key.elementName,
        attributes = mapOf(FlorisImeUi.Attr.Code to keyData.code),
        selector = if (!enabled) SnyggSelector.DISABLED else if (isPressed) SnyggSelector.PRESSED else SnyggSelector.NONE,
        modifier = modifier.padding(2.dp).alpha(if (enabled) 1f else DisabledAlpha),
        clickAndSemanticsModifier = Modifier
            .indication(interactionSource, ripple())
            .pointerInput(keyData, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.pressed != down.previousPressed) down.consume()
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    inputEventDispatcher.sendDown(
                        keyData,
                        repeat = keyData.code == KeyCode.SPACE &&
                            prefs.gestures.spaceBarLongPress.get() == SwipeAction.REPEAT_SPACE,
                    )
                    feedback.keyPress(keyData)
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        interactionSource.tryEmit(PressInteraction.Release(press))
                        inputEventDispatcher.sendUp(keyData)
                    } else {
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                        inputEventDispatcher.sendCancel(keyData)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (showDot) ActiveDot()
    }
}

/**
 * A key which looks like a normal keyboard key and runs [onClick] on tap. Uses a normal click handler so it also works
 * inside scrolling lists.
 */
@Composable
fun UiTapKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDot: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val feedback = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val selector = when {
        !enabled -> SnyggSelector.DISABLED
        isPressed -> SnyggSelector.PRESSED
        else -> SnyggSelector.NONE
    }
    SnyggBox(
        elementName = FlorisImeUi.Key.elementName,
        selector = selector,
        modifier = modifier.padding(2.dp).alpha(if (enabled) 1f else DisabledAlpha),
        clickAndSemanticsModifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = {
                feedback.keyPress(TextKeyData.UNSPECIFIED)
                onClick()
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (showDot) ActiveDot()
    }
}

/** The green "active" dot in the top corner of a key. */
@Composable
fun BoxScope.ActiveDot() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(5.dp)
            .size(8.dp)
            .background(ActiveDotColor, CircleShape),
    )
}

/** Label of a key with an optional icon on its left (icon is roughly text sized). */
@Composable
fun UiKeyLabel(text: String, icon: ImageVector? = null) {
    ProvideSnyggFontScale(0.55f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                SnyggIcon(
                    modifier = Modifier.size(15.dp),
                    imageVector = icon,
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(width = 3.dp, height = 1.dp))
            }
            SnyggText(text = text)
        }
    }
}
