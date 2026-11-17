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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One tappable value of a card or login. [value] is what is typed into the field, [shown] what is displayed. */
class VaultFieldView(val caption: String, val shown: String, val value: String)

/** A card or a login in a form both the keyboard panel and the settings screen can draw. */
class VaultEntryView(val id: String, val title: String, val hasImage: Boolean, val fields: List<VaultFieldView>)

@Composable
fun vaultCardViews(cards: List<VaultCard>): List<VaultEntryView> {
    val capHolder = uiString(R.string.ui__vault__holder)
    val capNumber = uiString(R.string.ui__vault__number)
    val capExpiry = uiString(R.string.ui__vault__expiry)
    return cards.map { c ->
        VaultEntryView(
            id = c.id,
            title = c.title.ifBlank { UiVault.maskedNumber(c.number) },
            hasImage = c.hasImage,
            fields = listOf(
                VaultFieldView(capHolder, c.holder, c.holder),
                VaultFieldView(capNumber, UiVault.maskedNumber(c.number), c.number),
                VaultFieldView(capExpiry, c.expiry, c.expiry),
            ).filter { it.value.isNotBlank() },
        )
    }
}

@Composable
fun vaultLoginViews(logins: List<VaultLogin>): List<VaultEntryView> {
    val capUser = uiString(R.string.ui__vault__username)
    val capPass = uiString(R.string.ui__vault__password)
    return logins.map { l ->
        VaultEntryView(
            id = l.id,
            title = l.site,
            hasImage = l.hasImage,
            fields = listOf(
                VaultFieldView(capUser, l.username, l.username),
                VaultFieldView(capPass, "••••••••", l.password),
            ).filter { it.value.isNotBlank() },
        )
    }
}

@Composable
private fun rememberVaultImage(id: String, has: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val version = UiVault.imageVersion
    val bmp by produceState<ImageBitmap?>(null, id, has, version) {
        value = if (!has) null else withContext(Dispatchers.IO) { UiVault.loadImage(context, id)?.asImageBitmap() }
    }
    return bmp
}

/**
 * Card drawn for one entry: the picture (if any) faded to 50% behind the title and the values.
 *
 * @param label draws a text (the keyboard uses the theme colours, the settings use Material text).
 * @param onField called with the real value when a value is tapped (null: values are not tappable).
 * @param headerEnd extra content at the end of the title row (the settings put their buttons there).
 */
@Composable
fun VaultEntryCard(
    entry: VaultEntryView,
    label: @Composable (text: String, bold: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onField: ((String) -> Unit)? = null,
    headerEnd: @Composable () -> Unit = {},
) {
    val image = rememberVaultImage(entry.id, entry.hasImage)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Gray.copy(alpha = 0.25f)),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.5f,
                modifier = Modifier.matchParentSize(),
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) { label(entry.title, true) }
                headerEnd()
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                entry.fields.forEach { field ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Gray.copy(alpha = 0.35f))
                            .let { if (onField != null) it.clickable { onField(field.value) } else it }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            label(field.caption, false)
                            label(field.shown, false)
                        }
                    }
                }
            }
        }
    }
}
