/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Dialogs
import org.thoughtcrime.securesms.keyvalue.SignalStore

/**
 * Asks for the Signal PIN before letting go of Accessibility Mode. Reached from the overflow button
 * on the simplified home screen -- the caregiver's way back into the full app.
 *
 * With no PIN on the device there is nothing to check, so the dialog says so and lets them leave.
 * That is not a weakening: without a PIN, Signal itself offers no secret to verify against.
 */
@Composable
fun AccessibilityExitDialog(
  onDismiss: () -> Unit,
  onLeave: () -> Unit
) {
  val hasPin = remember { SignalStore.svr.hasPin() }

  var pin by remember { mutableStateOf("") }
  val pinCheck = rememberPinCheck(pin)

  Dialogs.BaseAlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier,
    title = {
      Text(text = "Leave Accessibility Mode")
    },
    text = {
      Column {
        if (hasPin) {
          Text(
            text = "Enter your Signal PIN to switch back to the full app.",
            style = MaterialTheme.typography.bodyMedium
          )

          OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text(text = "Signal PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = pinCheck.failed,
            supportingText = {
              if (pinCheck.failed) {
                Text(text = "That is not your Signal PIN.")
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp)
          )
        } else {
          Text(
            text = "This device has no Signal PIN, so there is nothing to verify. Accessibility Mode will be switched off.",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = "Cancel")
      }
    },
    confirmButton = {
      TextButton(
        onClick = onLeave,
        enabled = !hasPin || pinCheck.verified
      ) {
        Text(text = "Leave")
      }
    }
  )
}
