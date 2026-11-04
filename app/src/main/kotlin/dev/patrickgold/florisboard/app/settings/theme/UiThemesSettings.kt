/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.settings.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.uikb.UiThemeDef
import dev.patrickgold.florisboard.ime.uikb.UiThemes
import dev.patrickgold.florisboard.ime.uikb.uiThemeTextColor
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.florisboard.lib.compose.stringRes
import java.io.File

private const val ThemeDirName = "ui_theme"
private const val MaxImageSide = 1600

private fun deleteThemeImages(context: Context) {
    File(context.filesDir, ThemeDirName).listFiles()?.forEach { it.delete() }
}

/** Decodes the image behind [uri] (downscaled, rotation respected) and stores it in the private app storage. */
private fun importThemeImage(context: Context, uri: Uri): String? {
    return try {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MaxImageSide) {
                    val scale = MaxImageSide.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MaxImageSide * 2) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
        }
        val dir = File(context.filesDir, ThemeDirName).apply { mkdirs() }
        val target = File(dir, "bg_${System.currentTimeMillis()}.jpg")
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        // Remove older pictures only once the new one is safely written
        dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
        target.absolutePath
    } catch (e: Throwable) {
        null
    }
}

/** The "Themes" section of the appearance settings. */
@Composable
fun UiThemesSettings() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val index by prefs.theme.uiThemeIndex.observeAsState()
    val imagePath by prefs.theme.uiThemeImagePath.observeAsState()
    val hasImage = index < 0 && imagePath.isNotBlank() && File(imagePath).exists()
    val invalidMessage = stringRes(R.string.ui__themes__invalid)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { importThemeImage(context, uri) }
                if (path == null) {
                    Toast.makeText(context, invalidMessage, Toast.LENGTH_SHORT).show()
                } else {
                    prefs.theme.uiThemeIndex.set(-1)
                    prefs.theme.uiThemeImagePath.set(path)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringRes(R.string.ui__themes__title),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        SectionLabel(stringRes(R.string.ui__themes__pick_image))
        CardsRow {
            // Same size as a ready-made theme card
            ThemeCard(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { launcher.launch(arrayOf("image/*")) },
                selected = hasImage,
                checkColor = Color.White,
            ) {
                if (hasImage) {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = File(imagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            modifier = Modifier.size(28.dp),
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringRes(R.string.ui__themes__picture),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f))
        }

        SectionLabel(stringRes(R.string.ui__themes__ready))
        UiThemes.chunked(3).forEachIndexed { rowIndex, row ->
            CardsRow {
                row.forEachIndexed { col, def ->
                    val themeIndex = rowIndex * 3 + col
                    ThemePreviewCard(
                        def = def,
                        selected = index == themeIndex,
                        onClick = {
                            deleteThemeImages(context)
                            scope.launch {
                                prefs.theme.uiThemeIndex.set(themeIndex)
                                prefs.theme.uiThemeImagePath.set("")
                            }
                        },
                    )
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }

        OutlinedButton(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onClick = {
                deleteThemeImages(context)
                scope.launch {
                    prefs.theme.uiThemeIndex.set(-1)
                    prefs.theme.uiThemeImagePath.set("")
                }
            },
            enabled = index >= 0 || imagePath.isNotBlank(),
            shape = RoundedCornerShape(50),
        ) { Text(stringRes(R.string.ui__themes__cancel)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CardsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        content = content,
    )
}

/** One card of the grid; every card (picture or ready-made theme) uses this, so all have the same size. */
@Composable
private fun RowScope.ThemeCard(
    modifier: Modifier = Modifier,
    selected: Boolean,
    checkColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp)
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(12.dp))
            .then(modifier),
    ) {
        content()
        if (selected) {
            Box(
                modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(44.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkColor,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ThemePreviewCard(def: UiThemeDef, selected: Boolean, onClick: () -> Unit) {
    ThemeCard(
        modifier = Modifier.background(def.background).clickable(onClick = onClick),
        selected = selected,
        checkColor = uiThemeTextColor(def.background),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(def.accent),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .aspectRatio(8f)
                .clip(RoundedCornerShape(50))
                .background(def.key),
        )
    }
}
