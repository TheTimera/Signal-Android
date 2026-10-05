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
  val allowAnswerWithoutVideo: Boolean = true,
  val speakerAlwaysOn: Boolean = true,
  val showMicLevel: Boolean = true,
  val allowScreenShare: Boolean = false,
  val screenShareAvailable: Boolean = false,
  val callStartVolumePercent: Int = 80,
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
}
