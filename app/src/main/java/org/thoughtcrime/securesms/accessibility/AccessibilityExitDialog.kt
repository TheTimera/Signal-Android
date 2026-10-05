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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Dialogs
import org.thoughtcrime.securesms.R
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

  // Once the PIN is right there is nothing left to type, and the keyboard would sit on top of the
  // button that is now enabled.
  val keyboard = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  LaunchedEffect(pinCheck.verified) {
    if (pinCheck.verified) {
      focusManager.clearFocus(force = true)
      keyboard?.hide()
    }
  }

  Dialogs.BaseAlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier,
    title = {
      Text(text = stringResource(R.string.Accessibility__leave_accessibility_mode))
    },
    text = {
      Column {
        if (hasPin) {
          Text(
            text = stringResource(R.string.Accessibility__enter_pin_to_switch_back),
            style = MaterialTheme.typography.bodyMedium
          )

          OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text(text = stringResource(R.string.Accessibility__signal_pin)) },
            singleLine = true,
            // Nothing left to type once it is right, and a locked field lets the keyboard close
            // instead of covering the button that just became available.
            enabled = !pinCheck.verified,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = pinCheck.failed,
            supportingText = {
              if (pinCheck.failed) {
                Text(text = stringResource(R.string.Accessibility__that_is_not_your_pin))
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp)
          )
        } else {
          Text(
            text = stringResource(R.string.Accessibility__no_pin_nothing_to_verify),
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(R.string.Accessibility__cancel))
      }
    },
    confirmButton = {
      TextButton(
        onClick = onLeave,
        enabled = !hasPin || pinCheck.verified
      ) {
        Text(text = stringResource(R.string.Accessibility__leave))
      }
    }
  )
}
