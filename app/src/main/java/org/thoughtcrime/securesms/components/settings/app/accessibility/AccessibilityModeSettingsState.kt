/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.runtime.Immutable

@Immutable
data class AccessibilityModeSettingsState(
  val enabled: Boolean = false,
  val iconsAlwaysVisible: Boolean = true,
  val allowCameraToggle: Boolean = false,
  val allowMicToggle: Boolean = false,
  val allowCameraSwitch: Boolean = false,
  val hasPin: Boolean = false,
  val exitWithPin: Boolean = true,
  val showActivationDialog: Boolean = false
) {
  /**
   * The PIN can only be the way out if one actually exists -- Signal does not always have one. When
   * it is, typing it is the precondition for turning the mode on.
   */
  val pinIsExitMethod: Boolean
    get() = exitWithPin && hasPin

  /**
   * Mirrors AccessibilityValues.isVideoCallOnly: the restriction holds as long as no exception has
   * been granted. Kept derived here too so the screen cannot show a state the store disagrees with.
   */
  val isVideoCallOnly: Boolean
    get() = !allowCameraToggle && !allowMicToggle && !allowCameraSwitch
}
