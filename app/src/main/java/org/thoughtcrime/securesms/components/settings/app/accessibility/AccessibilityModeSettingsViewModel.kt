/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.notifications.NotificationCancellationHelper
import org.thoughtcrime.securesms.util.RemoteConfig

class AccessibilityModeSettingsViewModel : ViewModel() {

  private val _state = MutableStateFlow(loadState())
  val state: StateFlow<AccessibilityModeSettingsState> = _state

  fun onEvent(event: AccessibilityModeSettingsEvents) {
    when (event) {
      is AccessibilityModeSettingsEvents.ToggleIconsAlwaysVisible -> {
        SignalStore.accessibility.iconsAlwaysVisible = event.enabled
        _state.value = _state.value.copy(iconsAlwaysVisible = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleAllowCameraToggle -> {
        SignalStore.accessibility.allowCameraToggle = event.enabled
        _state.value = _state.value.copy(allowCameraToggle = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleAllowMicToggle -> {
        SignalStore.accessibility.allowMicToggle = event.enabled
        _state.value = _state.value.copy(allowMicToggle = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleAllowCameraSwitch -> {
        SignalStore.accessibility.allowCameraSwitch = event.enabled
        _state.value = _state.value.copy(allowCameraSwitch = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleAllowAnswerWithoutVideo -> {
        SignalStore.accessibility.allowAnswerWithoutVideo = event.enabled
        _state.value = _state.value.copy(allowAnswerWithoutVideo = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleSpeakerAlwaysOn -> {
        SignalStore.accessibility.speakerAlwaysOn = event.enabled
        _state.value = _state.value.copy(speakerAlwaysOn = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleShowMicLevel -> {
        SignalStore.accessibility.showMicLevel = event.enabled
        _state.value = _state.value.copy(showMicLevel = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleHideMessageNotifications -> {
        SignalStore.accessibility.hideMessageNotifications = event.enabled
        _state.value = _state.value.copy(hideMessageNotifications = event.enabled)
      }
      is AccessibilityModeSettingsEvents.ToggleAllowScreenShare -> {
        SignalStore.accessibility.allowScreenShare = event.enabled
        _state.value = _state.value.copy(allowScreenShare = event.enabled)
      }
      is AccessibilityModeSettingsEvents.SetCallStartVolumePercent -> {
        SignalStore.accessibility.callStartVolumePercent = event.percent
        _state.value = _state.value.copy(callStartVolumePercent = event.percent)
      }
      AccessibilityModeSettingsEvents.RequestActivation -> {
        _state.value = _state.value.copy(showActivationDialog = true)
      }
      AccessibilityModeSettingsEvents.DismissActivationDialog -> {
        _state.value = _state.value.copy(showActivationDialog = false)
      }
      AccessibilityModeSettingsEvents.Activate -> {
        SignalStore.accessibility.isEnabled = true
        _state.value = _state.value.copy(enabled = true, showActivationDialog = false)

        // Notifications already on screen have to be cancelled here. DefaultMessageNotifier revokes
        // them as well, but it only runs when an event drives it, and turning the mode on is not
        // one -- an existing message notification would sit in the shade until the next call
        // happened to wake the notifier.
        if (SignalStore.accessibility.suppressesMessageNotifications) {
          NotificationCancellationHelper.cancelAllMessageNotifications(AppDependencies.application)
        }
      }
      AccessibilityModeSettingsEvents.Deactivate -> {
        SignalStore.accessibility.isEnabled = false
        _state.value = _state.value.copy(enabled = false)
      }
    }
  }

  /**
   * Re-reads the store. Needed because the caregiver can leave this screen to create a Signal PIN
   * or change the exit method and then come back -- without this the screen would still believe
   * there is no PIN and keep offering the wrong path.
   */
  fun refresh() {
    _state.value = loadState().copy(showActivationDialog = _state.value.showActivationDialog)
  }

  private fun loadState(): AccessibilityModeSettingsState {
    return AccessibilityModeSettingsState(
      enabled = SignalStore.accessibility.isEnabled,
      iconsAlwaysVisible = SignalStore.accessibility.iconsAlwaysVisible,
      allowCameraToggle = SignalStore.accessibility.allowCameraToggle,
      allowMicToggle = SignalStore.accessibility.allowMicToggle,
      allowCameraSwitch = SignalStore.accessibility.allowCameraSwitch,
      allowAnswerWithoutVideo = SignalStore.accessibility.allowAnswerWithoutVideo,
      speakerAlwaysOn = SignalStore.accessibility.speakerAlwaysOn,
      showMicLevel = SignalStore.accessibility.showMicLevel,
      hideMessageNotifications = SignalStore.accessibility.hideMessageNotifications,
      allowScreenShare = SignalStore.accessibility.allowScreenShare,
      callStartVolumePercent = SignalStore.accessibility.callStartVolumePercent,
      // The row stays off the page entirely when Signal's server has screen sharing switched off:
      // a toggle that can never do anything is worse than no toggle.
      screenShareAvailable = RemoteConfig.screenSharing,
      hasPin = SignalStore.svr.hasPin(),
      exitWithPin = SignalStore.accessibility.exitWithPin
    )
  }
}
