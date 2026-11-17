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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.FilterQuality
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One tappable value of a card or login. [value] is what is typed into the field, [shown] what is displayed. */
class VaultFieldView(val caption: String, val shown: String, val value: String)

/** A card or a login in a form both the keyboard panel and the settings screen can draw. */
class VaultEntryView(
    val id: String,
    val title: String,
    val hasImage: Boolean,
    val fields: List<VaultFieldView>,
    /** "VISA", "Mastercard" or "AMEX" for cards whose number is recognised, otherwise null. */
    val brand: String? = null,
)

/** Recognises the card network from the first digits of the number. */
fun vaultCardBrand(number: String): String? {
    val d = number.filter { it.isDigit() }
    if (d.isEmpty()) return null
    if (d[0] == '4') return "VISA"
    if (d.length >= 2) {
        val two = d.substring(0, 2).toInt()
        if (two == 34 || two == 37) return "AMEX"
        if (two in 51..55) return "Mastercard"
    }
    if (d.length >= 4) {
        val four = d.substring(0, 4).toInt()
        if (four in 2221..2720) return "Mastercard"
    }
    return null
}

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
            brand = vaultCardBrand(c.number),
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

/** A blurred copy of the entry's picture (like the keys on a keyboard with a picture background). */
@Composable
private fun rememberVaultBlurred(id: String, has: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val version = UiVault.imageVersion
    val bmp by produceState<ImageBitmap?>(null, id, has, version) {
        value = if (!has) null else withContext(Dispatchers.IO) {
            runCatching {
                val full = UiVault.loadImage(context, id) ?: return@runCatching null
                val scale = 240f / max(full.width, full.height)
                val w = max(1, (full.width * scale).roundToInt())
                val h = max(1, (full.height * scale).roundToInt())
                val small = android.graphics.Bitmap.createScaledBitmap(full, w, h, true)
                val pixels = IntArray(w * h)
                small.getPixels(pixels, 0, w, 0, 0, w, h)
                repeat(3) { gaussianPass(pixels, w, h, 8) }
                small.setPixels(pixels, 0, w, 0, 0, w, h)
                small.asImageBitmap()
            }.getOrNull()
        }
    }
    return bmp
}

/**
 * Background of a data cell or of the title pill: plain grey, or (when the entry has a picture) the blurred picture
 * aligned with the sharp one and darkened a little, like the keys on a keyboard with a picture background.
 */
@Composable
private fun Modifier.vaultCellBackground(blurred: ImageBitmap?, cardPos: Offset, cardSize: Size): Modifier {
    var cellPos by remember { mutableStateOf(Offset.Zero) }
    val positioned = this.onGloballyPositioned { cellPos = it.positionInRoot() }
    if (blurred == null) return positioned.background(Color.Gray.copy(alpha = 0.35f))
    return positioned.drawBehind {
        if (cardSize.width <= 0f || cardSize.height <= 0f) return@drawBehind
        val scale = max(cardSize.width / blurred.width, cardSize.height / blurred.height)
        val dstW = blurred.width * scale
        val dstH = blurred.height * scale
        translate(
            left = cardPos.x - cellPos.x + (cardSize.width - dstW) / 2f,
            top = cardPos.y - cellPos.y + (cardSize.height - dstH) / 2f,
        ) {
            drawImage(
                image = blurred,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(blurred.width, blurred.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
        drawRect(Color.Black.copy(alpha = 0.36f))
    }
}

/**
 * Card drawn for one entry: the picture (if any) faded to 50% behind the title pill and the values.
 *
 * @param label draws a text (the keyboard uses the theme colours, the settings use Material text).
 * @param onField called with the real value when a value is tapped (null: values are not tappable).
 * @param headerEnd extra content at the left of the title row (the settings put their buttons there).
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
    val blurred = rememberVaultBlurred(entry.id, entry.hasImage)
    val shape = RoundedCornerShape(16.dp)
    var cardPos by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(Size.Zero) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .onGloballyPositioned {
                cardPos = it.positionInRoot()
                cardSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
            }
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
            // Title row: always laid out left to right, so the pill is in the right third on any language
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.CenterStart) { headerEnd() }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val arabicName = entry.title.any { it in '\u0600'..'\u06FF' }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .vaultCellBackground(blurred, cardPos, cardSize)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val badge: @Composable () -> Unit = {
                                if (entry.brand != null) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                    ) { label(entry.brand, false) }
                                }
                            }
                            // Arabic name: badge at the left of the text; other names: badge at the right
                            if (arabicName) badge()
                            Box(modifier = Modifier.weight(1f, fill = false)) { label(entry.title, true) }
                            if (!arabicName) badge()
                        }
                    }
                }
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
                            .vaultCellBackground(blurred, cardPos, cardSize)
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
