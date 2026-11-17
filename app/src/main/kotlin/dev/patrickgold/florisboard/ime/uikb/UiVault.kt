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

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class VaultCard(
    val id: String,
    val title: String = "",
    val holder: String = "",
    val number: String = "",
    val expiry: String = "",
    val hasImage: Boolean = false,
)

@Serializable
data class VaultLogin(
    val id: String,
    val site: String = "",
    val username: String = "",
    val password: String = "",
    val hasImage: Boolean = false,
)

@Serializable
data class VaultData(
    val cards: List<VaultCard> = emptyList(),
    val logins: List<VaultLogin> = emptyList(),
)

/**
 * The saved payment cards and website logins. Everything is stored on the phone only, encrypted (AES-GCM) with a key
 * that lives in the Android Keystore. The key is NOT bound to the fingerprint / screen lock, so the data survives
 * changes of the lock; the fingerprint / pattern is asked by the app before anything is shown (see
 * [UiVaultAuthActivity]). The data is kept in memory only while the keyboard panel or the settings screen is open.
 */
object UiVault {
    private const val KEY_ALIAS = "ui_vault_key"
    private const val FILE_NAME = "ui_vault.bin"
    private const val IMAGE_DIR = "ui_vault_images"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** The decrypted data, only filled while [imeUnlocked] or [settingsUnlocked]. */
    var data by mutableStateOf(VaultData())
        private set

    var imeUnlocked by mutableStateOf(false)
        private set
    var settingsUnlocked by mutableStateOf(false)
        private set

    /** True if the saved file exists but could not be decrypted (for example the key was lost). */
    var unreadable by mutableStateOf(false)
        private set

    /** Increases when an image changed, so the pictures are loaded again. */
    var imageVersion by mutableIntStateOf(0)
        private set

    /** The keyboard asked for the panel and the keyboard window is away for the fingerprint screen. */
    var pendingPanel = false
    private var imeUnlockedAt = 0L

    // ---- Opening and closing ----

    /** Called from the keyboard: opens the panel, after the fingerprint / pattern was accepted. */
    fun requestFromKeyboard(context: Context) {
        if (imeUnlocked) {
            UiPanelState.panel = UiPanel.VAULT
            return
        }
        pendingPanel = true
        launchAuth(context, "ime")
    }

    fun launchAuth(context: Context, target: String) {
        val intent = Intent(context, UiVaultAuthActivity::class.java)
            .putExtra(UiVaultAuthActivity.EXTRA_TARGET, target)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Called by the auth screen after a successful check. */
    fun unlock(context: Context, target: String) {
        if (!imeUnlocked && !settingsUnlocked) load(context.applicationContext)
        if (target == "ime") {
            imeUnlocked = true
            imeUnlockedAt = SystemClock.uptimeMillis()
            if (pendingPanel) UiPanelState.panel = UiPanel.VAULT
        } else {
            settingsUnlocked = true
        }
    }

    fun lockIme() {
        imeUnlocked = false
        pendingPanel = false
        clearIfClosed()
    }

    fun lockSettings() {
        settingsUnlocked = false
        clearIfClosed()
    }

    /** The keyboard window was hidden. The short delay keeps the fingerprint screen itself from locking again. */
    fun onKeyboardHidden() {
        if (imeUnlocked && SystemClock.uptimeMillis() - imeUnlockedAt > 2000) lockIme()
    }

    private fun clearIfClosed() {
        if (!imeUnlocked && !settingsUnlocked) {
            data = VaultData()
            unreadable = false
        }
    }

    // ---- Encryption ----

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun load(context: Context) {
        val f = file(context)
        if (!f.exists()) {
            data = VaultData()
            unreadable = false
            return
        }
        try {
            val bytes = f.readBytes()
            val iv = bytes.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(bytes, 12, bytes.size - 12)
            data = json.decodeFromString(VaultData.serializer(), String(plain, Charsets.UTF_8))
            unreadable = false
        } catch (e: Exception) {
            data = VaultData()
            unreadable = true
        }
    }

    private fun save(context: Context, newData: VaultData) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json.encodeToString(VaultData.serializer(), newData).toByteArray(Charsets.UTF_8))
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeBytes(cipher.iv + encrypted)
        tmp.renameTo(file(context))
        data = newData
        unreadable = false
    }

    // ---- Changes (only while unlocked) ----

    fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16)

    fun upsertCard(context: Context, card: VaultCard) {
        val list = data.cards
        val updated = if (list.any { it.id == card.id }) list.map { if (it.id == card.id) card else it } else list + card
        save(context.applicationContext, data.copy(cards = updated))
    }

    fun upsertLogin(context: Context, login: VaultLogin) {
        val list = data.logins
        val updated = if (list.any { it.id == login.id }) list.map { if (it.id == login.id) login else it } else list + login
        save(context.applicationContext, data.copy(logins = updated))
    }

    fun deleteCard(context: Context, id: String) {
        deleteImageFile(context, id)
        save(context.applicationContext, data.copy(cards = data.cards.filter { it.id != id }))
    }

    fun deleteLogin(context: Context, id: String) {
        deleteImageFile(context, id)
        save(context.applicationContext, data.copy(logins = data.logins.filter { it.id != id }))
    }

    // ---- Pictures ----

    private fun imageFile(context: Context, id: String) = File(File(context.filesDir, IMAGE_DIR), "$id.jpg")

    private fun deleteImageFile(context: Context, id: String) {
        runCatching { imageFile(context, id).delete() }
        imageVersion++
    }

    /** Copies the picked picture into the app folder, scaled down. Returns true on success. */
    fun saveImage(context: Context, id: String, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 900 || bounds.outHeight / (sample * 2) >= 900) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return false
            val f = imageFile(context, id)
            f.parentFile?.mkdirs()
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            imageVersion++
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadImage(context: Context, id: String): Bitmap? {
        return runCatching {
            val f = imageFile(context, id)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
        }.getOrNull()
    }

    fun removeImageFile(context: Context, id: String) = deleteImageFile(context, id)

    // ---- Display helpers ----

    fun maskedNumber(number: String): String {
        val digits = number.filter { it.isLetterOrDigit() }
        return if (digits.length <= 4) digits else "•••• " + digits.takeLast(4)
    }
}
