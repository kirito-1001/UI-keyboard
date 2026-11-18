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

package dev.patrickgold.florisboard.app.settings.vault

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.patrickgold.jetpref.datastore.model.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.patrickgold.florisboard.R
import kotlinx.coroutines.launch
import dev.patrickgold.florisboard.ime.uikb.UiVault
import dev.patrickgold.florisboard.ime.uikb.UiVaultAuthActivity
import dev.patrickgold.florisboard.ime.uikb.VaultCard
import dev.patrickgold.florisboard.ime.uikb.VaultEntryCard
import dev.patrickgold.florisboard.ime.uikb.VaultLogin
import dev.patrickgold.florisboard.ime.uikb.vaultCardViews
import dev.patrickgold.florisboard.ime.uikb.vaultLoginViews
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import org.florisboard.lib.compose.stringRes

@Composable
fun VaultScreen() = FlorisScreen {
    title = stringRes(R.string.ui__vault__title)
    previewFieldVisible = false

    content {
        VaultScreenBody()
    }
}

@Composable
private fun VaultScreenBody() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var authRunning by remember { mutableStateOf(false) }
    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        authRunning = false
    }
    fun askAuth() {
        if (authRunning || UiVault.settingsUnlocked) return
        if (UiVault.checkedRecently()) {
            UiVault.unlock(context, "settings")
            return
        }
        authRunning = true
        runCatching {
            authLauncher.launch(
                Intent(context, UiVaultAuthActivity::class.java).putExtra(UiVaultAuthActivity.EXTRA_TARGET, "settings")
            )
        }.onFailure { authRunning = false }
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Asks for the fingerprint / lock when the screen comes to the front, and locks again when the app is left
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Not at once: the screen is still opening (from the keyboard or the app list)
                scope.launch {
                    kotlinx.coroutines.delay(450)
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) askAuth()
                }
            }
            if (event == Lifecycle.Event.ON_STOP && !UiVault.pickerOpen) UiVault.lockSettings()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            UiVault.lockSettings()
        }
    }

    var tab by remember { mutableIntStateOf(0) }
    var showDuration by remember { mutableStateOf(false) }
    val lockPrefs by dev.patrickgold.florisboard.app.FlorisPreferenceStore
    val secondsPref = lockPrefs.smartbar.uiVaultLockSeconds
    val seconds by secondsPref.observeAsState()
    var editCard by remember { mutableStateOf<VaultCard?>(null) }
    var editLogin by remember { mutableStateOf<VaultLogin?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var pickFor by remember { mutableStateOf<String?>(null) }
    var pickIsCard by remember { mutableStateOf(true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        UiVault.pickerOpen = false
        val id = pickFor
        pickFor = null
        if (uri != null && id != null) {
            if (UiVault.saveImage(context, id, uri)) {
                runCatching {
                    if (pickIsCard) {
                        UiVault.data.cards.firstOrNull { it.id == id }?.let { UiVault.upsertCard(context, it.copy(hasImage = true)) }
                    } else {
                        UiVault.data.logins.firstOrNull { it.id == id }?.let { UiVault.upsertLogin(context, it.copy(hasImage = true)) }
                    }
                }
            }
        }
    }
    fun pickImage(id: String, isCard: Boolean) {
        pickFor = id
        pickIsCard = isCard
        UiVault.pickerOpen = true
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    if (!UiVault.settingsUnlocked) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringRes(R.string.ui__vault__locked), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { askAuth() }) { Text(stringRes(R.string.ui__vault__unlock)) }
        }
        return
    }


    val label: @Composable (String, Boolean) -> Unit = { text, bold ->
        Text(
            text = text,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.LocalContentColor.current,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            if (tab == 0) {
                Button(onClick = { tab = 0 }) { Text(stringRes(R.string.ui__vault__tab_cards)) }
                OutlinedButton(onClick = { tab = 1 }) { Text(stringRes(R.string.ui__vault__tab_logins)) }
            } else {
                OutlinedButton(onClick = { tab = 0 }) { Text(stringRes(R.string.ui__vault__tab_cards)) }
                Button(onClick = { tab = 1 }) { Text(stringRes(R.string.ui__vault__tab_logins)) }
            }
            // At the end of the row: the left side on a right-to-left screen
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showDuration = true }) {
                Text(stringRes(R.string.ui__vault__lock_duration) + ": " + seconds + " " + stringRes(R.string.ui__vault__seconds_short))
            }
        }
        if (UiVault.unreadable) {
            Text(
                text = stringRes(R.string.ui__vault__unreadable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = stringRes(R.string.ui__vault__image_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tab == 0) {
            Button(onClick = { editCard = VaultCard(id = UiVault.newId()) }) {
                Text(stringRes(R.string.ui__vault__add_card))
            }
        } else {
            Button(onClick = { editLogin = VaultLogin(id = UiVault.newId()) }) {
                Text(stringRes(R.string.ui__vault__add_login))
            }
        }

        val entries = if (tab == 0) vaultCardViews(UiVault.data.cards) else vaultLoginViews(UiVault.data.logins)
        if (entries.isEmpty()) {
            Text(text = stringRes(R.string.ui__vault__empty), style = MaterialTheme.typography.bodyMedium)
        }
        entries.forEach { entry ->
            VaultEntryCard(
                entry = entry,
                label = label,
                modifier = Modifier.fillMaxWidth(),
                headerEnd = {
                    Row {
                        androidx.compose.foundation.layout.Box {
                            TextButton(onClick = {
                                if (entry.hasImage) menuFor = entry.id else pickImage(entry.id, tab == 0)
                            }) { Text(stringRes(R.string.ui__vault__background)) }
                            DropdownMenu(expanded = menuFor == entry.id, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(
                                    text = { Text(stringRes(R.string.ui__vault__change)) },
                                    onClick = { menuFor = null; pickImage(entry.id, tab == 0) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringRes(R.string.ui__vault__remove)) },
                                    onClick = {
                                        menuFor = null
                                        UiVault.removeImageFile(context, entry.id)
                                        if (tab == 0) {
                                            UiVault.data.cards.firstOrNull { it.id == entry.id }
                                                ?.let { UiVault.upsertCard(context, it.copy(hasImage = false)) }
                                        } else {
                                            UiVault.data.logins.firstOrNull { it.id == entry.id }
                                                ?.let { UiVault.upsertLogin(context, it.copy(hasImage = false)) }
                                        }
                                    },
                                )
                            }
                        }
                        TextButton(onClick = {
                            if (tab == 0) editCard = UiVault.data.cards.firstOrNull { it.id == entry.id }
                            else editLogin = UiVault.data.logins.firstOrNull { it.id == entry.id }
                        }) { Text(stringRes(R.string.ui__vault__edit)) }
                    }
                },
            )
        }
    }

    editCard?.let { card ->
        var title by remember(card.id) { mutableStateOf(card.title) }
        var holder by remember(card.id) { mutableStateOf(card.holder) }
        var number by remember(card.id) { mutableStateOf(card.number) }
        var expiry by remember(card.id) { mutableStateOf(card.expiry) }
        val exists = UiVault.data.cards.any { it.id == card.id }
        AlertDialog(
            onDismissRequest = { editCard = null },
            title = { Text(stringRes(R.string.ui__vault__tab_cards)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text(stringRes(R.string.ui__vault__card_title)) }, singleLine = true)
                    OutlinedTextField(holder, { holder = it }, label = { Text(stringRes(R.string.ui__vault__card_holder)) }, singleLine = true)
                    OutlinedTextField(
                        number, { number = it },
                        label = { Text(stringRes(R.string.ui__vault__card_number)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(expiry, { expiry = it }, label = { Text(stringRes(R.string.ui__vault__card_expiry)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        UiVault.upsertCard(context, card.copy(title = title.trim(), holder = holder.trim(), number = number.trim(), expiry = expiry.trim()))
                    }
                    editCard = null
                }) { Text(stringRes(R.string.ui__vault__save)) }
            },
            dismissButton = {
                Row {
                    if (exists) {
                        TextButton(onClick = {
                            runCatching { UiVault.deleteCard(context, card.id) }
                            editCard = null
                        }) { Text(stringRes(R.string.ui__vault__delete), color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { editCard = null }) { Text(stringRes(R.string.ui__vault__cancel)) }
                }
            },
        )
    }

    editLogin?.let { login ->
        var site by remember(login.id) { mutableStateOf(login.site) }
        var user by remember(login.id) { mutableStateOf(login.username) }
        var pass by remember(login.id) { mutableStateOf(login.password) }
        var show by remember(login.id) { mutableStateOf(false) }
        val exists = UiVault.data.logins.any { it.id == login.id }
        AlertDialog(
            onDismissRequest = { editLogin = null },
            title = { Text(stringRes(R.string.ui__vault__tab_logins)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(site, { site = it }, label = { Text(stringRes(R.string.ui__vault__site)) }, singleLine = true)
                    OutlinedTextField(user, { user = it }, label = { Text(stringRes(R.string.ui__vault__username)) }, singleLine = true)
                    OutlinedTextField(
                        pass, { pass = it },
                        label = { Text(stringRes(R.string.ui__vault__password)) },
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { show = !show }) {
                                Text(stringRes(if (show) R.string.ui__vault__hide else R.string.ui__vault__show))
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        UiVault.upsertLogin(context, login.copy(site = site.trim(), username = user.trim(), password = pass))
                    }
                    editLogin = null
                }) { Text(stringRes(R.string.ui__vault__save)) }
            },
            dismissButton = {
                Row {
                    if (exists) {
                        TextButton(onClick = {
                            runCatching { UiVault.deleteLogin(context, login.id) }
                            editLogin = null
                        }) { Text(stringRes(R.string.ui__vault__delete), color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { editLogin = null }) { Text(stringRes(R.string.ui__vault__cancel)) }
                }
            },
        )
    }

    if (showDuration) {
        var value by remember { mutableIntStateOf(seconds.coerceIn(5, 60)) }
        AlertDialog(
            onDismissRequest = { showDuration = false },
            title = { Text(stringRes(R.string.ui__vault__lock_duration)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringRes(R.string.ui__vault__lock_duration_help), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        value.toString() + " " + stringRes(R.string.ui__vault__seconds_short),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    androidx.compose.material3.Slider(
                        value = value.toFloat(),
                        onValueChange = { value = it.toInt().coerceIn(5, 60) },
                        valueRange = 5f..60f,
                        steps = 54,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { secondsPref.set(value) }
                    showDuration = false
                }) { Text(stringRes(R.string.ui__vault__save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDuration = false }) { Text(stringRes(R.string.ui__vault__cancel)) }
            },
        )
    }
}
