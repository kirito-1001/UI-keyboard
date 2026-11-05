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

package dev.patrickgold.florisboard.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import org.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.R

internal val BudokanFont = FontFamily(Font(R.font.budokan_rounded_bold))

/**
 * Branded opening screen: logo (large, uncropped), app name, then the faded developer credit, all centred as one
 * group in the middle of the screen.
 */
@Composable
fun UiSplashScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { /* swallow touches while the opening screen is shown */ },
        contentAlignment = Alignment.Center,
    ) {
        val logoSize = min(maxWidth * 0.58f, maxHeight * 0.34f)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ui_splash_logo),
                contentDescription = null,
                modifier = Modifier.size(logoSize),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(30.dp))
            Text(
                text = "UI Keyboard",
                fontFamily = BudokanFont,
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = stringRes(R.string.ui__splash__developed_by),
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(0.55f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Khaled Yagami",
                fontFamily = BudokanFont,
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(0.55f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
