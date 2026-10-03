/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Dialogs
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.whispersystems.signalservice.api.kbs.PinHashUtil

/** Signal enforces a minimum PIN length; below it there is nothing worth checking. */
private const val MIN_PIN_LENGTH = 4

/**
 * The pre-activation dialog from the design (screen 144).
 *
 * "Understood" only becomes available once a way back out is guaranteed:
 *  - PIN is the chosen exit -> the caregiver has to type it correctly first.
 *  - No PIN on the device -> the tap pattern is the only option, so the dialog says so and offers
 *    both paths: configure the pattern, or create a PIN and switch to the PIN method.
 *  - PIN exists but the pattern was chosen -> nothing to verify, the dialog only warns.
 */
@Composable
fun AccessibilityActivationDialog(
  hasPin: Boolean,
  pinIsExitMethod: Boolean,
  onDismiss: () -> Unit,
  onMoreInformation: () -> Unit,
  onSetUpTapPattern: () -> Unit,
  onCreatePin: () -> Unit,
  onConfirm: () -> Unit
) {
  var pin by remember { mutableStateOf("") }
  var verified by remember { mutableStateOf(false) }
  var lastAttemptFailed by remember { mutableStateOf(false) }

  Dialogs.BaseAlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier,
    title = {
      Text(text = "This is Accessibility Mode")
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        when {
          pinIsExitMethod -> {
            Text(
              text = "Enter your Signal PIN to confirm you know it. It is the only way back out of Accessibility Mode unless you switch to the tap pattern.",
              style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
              value = pin,
              onValueChange = {
                pin = it
                verified = false
                lastAttemptFailed = false
              },
              label = { Text(text = "Signal PIN") },
              singleLine = true,
              enabled = !verified,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              isError = lastAttemptFailed,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(
                onClick = {
                  // Deliberately on a button press rather than per keystroke: this runs Argon2, so
                  // checking on every character typed would make the field stutter.
                  val hash = SignalStore.svr.localPinHash
                  val ok = hash != null && PinHashUtil.verifyLocalPinHash(hash, pin)
                  verified = ok
                  lastAttemptFailed = !ok
                },
                enabled = pin.length >= MIN_PIN_LENGTH && !verified
              ) {
                Text(text = if (verified) "PIN confirmed" else "Verify")
              }

              if (lastAttemptFailed) {
                Text(
                  text = "That is not your Signal PIN.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          }

          !hasPin -> {
            Text(
              text = "You have no Signal PIN. The tap pattern is therefore your only way back out of Accessibility Mode. It is already set to seven taps in the top right corner within two seconds.",
              style = MaterialTheme.typography.bodyMedium
            )

            TextButton(
              onClick = onSetUpTapPattern,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Text(text = "Change the tap pattern")
            }

            Text(
              text = "You can also create a Signal PIN and use the PIN method instead.",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(top = 8.dp)
            )

            TextButton(
              onClick = onCreatePin,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Text(text = "Create a Signal PIN")
            }
          }

          else -> {
            Text(
              text = "The tap pattern is your way back out of Accessibility Mode. You can change it under \"How to exit Accessibility Mode\".",
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        Text(
          text = "If you forget your Signal PIN there is no way to recover it. You would have to reset Signal and lose its data.",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          fontStyle = FontStyle.Italic,
          modifier = Modifier.padding(top = 16.dp)
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onMoreInformation) {
        Text(text = "Need more information?")
      }
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        enabled = verified || !pinIsExitMethod
      ) {
        Text(text = "Understood")
      }
    }
  )
}
