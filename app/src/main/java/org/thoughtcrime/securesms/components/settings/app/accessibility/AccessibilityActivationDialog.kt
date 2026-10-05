/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Dialogs
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.accessibility.rememberPinCheck

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
      Text(text = stringResource(R.string.Accessibility__this_is_accessibility_mode))
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        when {
          pinIsExitMethod -> {
            Text(
              text = stringResource(R.string.Accessibility__enter_pin_to_confirm),
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
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
            )

            // No "Verify" button: rememberPinCheck watches the field and "Understood" stays
            // disabled until the PIN is right.
            if (pinCheck.failed) {
              Text(
                text = stringResource(R.string.Accessibility__that_is_not_your_pin),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }

          !hasPin -> {
            Text(
              text = stringResource(R.string.Accessibility__no_pin_tap_pattern_only),
              style = MaterialTheme.typography.bodyMedium
            )

            TextButton(
              onClick = onSetUpTapPattern,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Text(text = stringResource(R.string.Accessibility__change_the_tap_pattern))
            }

            Text(
              text = stringResource(R.string.Accessibility__or_create_a_pin),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(top = 8.dp)
            )

            TextButton(
              onClick = onCreatePin,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Text(text = stringResource(R.string.Accessibility__create_a_signal_pin))
            }
          }

          else -> {
            Text(
              text = stringResource(R.string.Accessibility__tap_pattern_is_your_way_out),
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        // Only where the PIN is the way back out. With the tap pattern chosen it warns about a
        // secret that has nothing to do with leaving the mode.
        if (pinIsExitMethod) {
          Text(
            text = stringResource(R.string.Accessibility__forgotten_pin_warning),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 16.dp)
          )
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onMoreInformation) {
        Text(text = stringResource(R.string.Accessibility__need_more_information))
      }
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        enabled = pinCheck.verified || !pinIsExitMethod
      ) {
        Text(text = stringResource(R.string.Accessibility__got_it))
      }
    }
  )
}
