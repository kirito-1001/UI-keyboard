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
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.uikb.UiIcons
import dev.patrickgold.jetpref.datastore.model.observeAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.LocalSnyggFontOverride
import org.florisboard.lib.snygg.ui.SnyggFontOverride
import java.io.File

private const val MinFontSizeDelta = -6
private const val MaxFontSizeDelta = 10
private const val FontDirName = "keyboard_font"

/** Loads the font family of the font file at [path], or null if there is no valid font. */
fun loadKeyboardFontFamily(path: String): FontFamily? {
    if (path.isBlank()) return null
    return try {
        val file = File(path)
        if (!file.exists()) return null
        Typeface.Builder(file).build()?.let { FontFamily(it) }
    } catch (e: Exception) {
        null
    }
}

/**
 * Copies the font behind [uri] into the private storage of the app, so it keeps working if the original is moved.
 * Returns the path of the copy and the display name of the font, or null if the file is not a valid ttf/otf font.
 */
private fun importKeyboardFont(context: Context, uri: Uri): Pair<String, String>? {
    try {
        val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: return null
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension != "ttf" && extension != "otf") return null
        val dir = File(context.filesDir, FontDirName).apply { mkdirs() }
        val temp = File(dir, "import.tmp")
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        if (!copied || Typeface.Builder(temp).build() == null) {
            temp.delete()
            return null
        }
        dir.listFiles()?.filter { it.name.startsWith("font_") }?.forEach { it.delete() }
        // A new file name for every import, so a changed font is always reloaded
        val target = File(dir, "font_${System.currentTimeMillis()}.$extension")
        if (!temp.renameTo(target)) {
            temp.delete()
            return null
        }
        return target.absolutePath to displayName.substringBeforeLast('.')
    } catch (e: Exception) {
        return null
    }
}

private fun deleteKeyboardFontFiles(context: Context) {
    File(context.filesDir, FontDirName).listFiles()?.forEach { it.delete() }
}

/**
 * Applies the chosen keyboard font and font size change to all texts (letters, numbers, symbols) shown inside of
 * [content].
 */
@Composable
fun ProvideKeyboardFont(content: @Composable () -> Unit) {
    val prefs by FlorisPreferenceStore
    val path by prefs.theme.keyboardFontPath.observeAsState()
    val delta by prefs.theme.keyboardFontSizeDelta.observeAsState()
    val family = remember(path) { loadKeyboardFontFamily(path) }
    val override = remember(family, delta) {
        if (family == null && delta == 0) null else SnyggFontOverride(family, delta.toFloat())
    }
    CompositionLocalProvider(LocalSnyggFontOverride provides override, content = content)
}

/** The "Keyboard font" section of the appearance settings. */
@Composable
fun KeyboardFontSettings() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val path by prefs.theme.keyboardFontPath.observeAsState()
    val name by prefs.theme.keyboardFontName.observeAsState()
    val delta by prefs.theme.keyboardFontSizeDelta.observeAsState()
    val family = remember(path) { loadKeyboardFontFamily(path) }
    val invalidMessage = stringRes(R.string.ui__font__invalid)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported = importKeyboardFont(context, uri)
            if (imported == null) {
                Toast.makeText(context, invalidMessage, Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    prefs.theme.keyboardFontPath.set(imported.first)
                    prefs.theme.keyboardFontName.set(imported.second)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Same colour as the icons of the other settings rows
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = UiIcons.FontSquare,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = stringRes(R.string.ui__font__title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringRes(R.string.ui__font__summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Preview box: shows the name of the chosen font in that font; tap to choose another one
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { launcher.launch(arrayOf("*/*")) }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (name.isBlank() || family == null) {
                Text(
                    text = stringRes(R.string.ui__font__pick_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = name,
                    fontFamily = family,
                    fontSize = (20 + delta).coerceAtLeast(8).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { scope.launch { prefs.theme.keyboardFontSizeDelta.set(delta + 1) } },
                enabled = delta < MaxFontSizeDelta,
                shape = RoundedCornerShape(50),
            ) { Text("+") }
            OutlinedButton(
                onClick = { scope.launch { prefs.theme.keyboardFontSizeDelta.set(delta - 1) } },
                enabled = delta > MinFontSizeDelta,
                shape = RoundedCornerShape(50),
            ) { Text("−") }
            Text(
                text = stringRes(R.string.ui__font__size) + ": " + (if (delta > 0) "+$delta" else "$delta"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    deleteKeyboardFontFiles(context)
                    scope.launch {
                        prefs.theme.keyboardFontPath.set("")
                        prefs.theme.keyboardFontName.set("")
                        prefs.theme.keyboardFontSizeDelta.set(0)
                    }
                },
                enabled = path.isNotBlank() || delta != 0,
                shape = RoundedCornerShape(50),
            ) { Text(stringRes(R.string.ui__font__reset)) }
        }
    }
}
