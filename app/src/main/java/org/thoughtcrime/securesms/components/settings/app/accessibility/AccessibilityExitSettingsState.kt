/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.runtime.Immutable
import org.thoughtcrime.securesms.keyvalue.AccessibilityTapCorner

@Immutable
data class AccessibilityExitSettingsState(
  val exitWithPin: Boolean = true,
  val hasPin: Boolean = false,
  val tapCorner: AccessibilityTapCorner = AccessibilityTapCorner.TOP_RIGHT,
  val tapCount: Int = 7,
  val tapWindowMillis: Int = 2000
) {
  /**
   * The PIN can only be the way out if one actually exists. Signal does not always have a PIN: it
   * can be skipped during setup, and there is an explicit opt-out state.
   */
  val pinMethodUsable: Boolean
    get() = exitWithPin && hasPin

  /** With the PIN method off -- or unusable -- the tap pattern is what gets the caregiver back in. */
  val tapMethodActive: Boolean
    get() = !pinMethodUsable
}
