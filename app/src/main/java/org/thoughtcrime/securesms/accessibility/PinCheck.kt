/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.whispersystems.signalservice.api.kbs.PinHashUtil

/** Signal enforces a minimum PIN length; below it there is nothing worth checking. */
const val MIN_PIN_LENGTH = 4

private const val DEBOUNCE_MILLIS = 300L

/**
 * What the field knows about what has been typed into it so far.
 *
 * [failed] stays false while someone is still typing, so the dialog does not call a half-typed PIN
 * wrong.
 */
@Immutable
data class PinCheck(
  val verified: Boolean = false,
  val checking: Boolean = false,
  val failed: Boolean = false
)

/**
 * Checks the Signal PIN as it is typed, so the dialogs need no "Verify" button and can simply keep
 * their confirm button disabled until [PinCheck.verified].
 *
 * The button existed for a reason: [PinHashUtil.verifyLocalPinHash] runs Argon2, which takes long
 * enough to stutter the text field if called on the main thread for every character. Hence both
 * guards here -- a debounce, so a fast typist triggers one run rather than six, and the work itself
 * on [Dispatchers.Default]. The LaunchedEffect is keyed on the pin, so any run still waiting or in
 * flight is cancelled the moment the next character arrives.
 */
@Composable
fun rememberPinCheck(pin: String): PinCheck {
  var state by remember { mutableStateOf(PinCheck()) }

  LaunchedEffect(pin) {
    if (pin.length < MIN_PIN_LENGTH) {
      state = PinCheck()
      return@LaunchedEffect
    }

    state = PinCheck(checking = true)
    delay(DEBOUNCE_MILLIS)

    val hash = SignalStore.svr.localPinHash
    val ok = hash != null && withContext(Dispatchers.Default) { PinHashUtil.verifyLocalPinHash(hash, pin) }

    state = PinCheck(verified = ok, failed = !ok)
  }

  return state
}
