/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

sealed interface AccessibilityModeSettingsEvents {
  data class ToggleIconsAlwaysVisible(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class ToggleAllowCameraToggle(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class ToggleAllowMicToggle(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class ToggleAllowCameraSwitch(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class ToggleAllowAnswerWithoutVideo(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class ToggleSpeakerAlwaysOn(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class ToggleAllowScreenShare(val enabled: Boolean) : AccessibilityModeSettingsEvents
  data class SetCallStartVolumePercent(val percent: Int) : AccessibilityModeSettingsEvents
  data object RequestActivation : AccessibilityModeSettingsEvents
  data object DismissActivationDialog : AccessibilityModeSettingsEvents
  data object Activate : AccessibilityModeSettingsEvents
  data object Deactivate : AccessibilityModeSettingsEvents
}
