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

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.patrickgold.florisboard.R

/**
 * A transparent screen that shows the system fingerprint / screen lock check (a keyboard cannot show it itself).
 * After success the saved cards and logins are unlocked for the caller given in [EXTRA_TARGET] ("ime" or "settings").
 */
class UiVaultAuthActivity : FragmentActivity() {
    companion object {
        const val EXTRA_TARGET = "target"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(EXTRA_TARGET) ?: "ime"
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.ui__vault__no_lock, Toast.LENGTH_LONG).show()
            UiVault.pendingPanel = false
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                UiVault.unlock(applicationContext, target)
                setResult(Activity.RESULT_OK)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                UiVault.pendingPanel = false
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.ui__vault__auth_title))
            .setSubtitle(getString(R.string.ui__vault__auth_subtitle))
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}
