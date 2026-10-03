/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.thoughtcrime.securesms.keyvalue.SignalStore

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
      AccessibilityModeSettingsEvents.RequestActivation -> {
        _state.value = _state.value.copy(showActivationDialog = true)
      }
      AccessibilityModeSettingsEvents.DismissActivationDialog -> {
        _state.value = _state.value.copy(showActivationDialog = false)
      }
      AccessibilityModeSettingsEvents.Activate -> {
        SignalStore.accessibility.isEnabled = true
        _state.value = _state.value.copy(enabled = true, showActivationDialog = false)
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
      hasPin = SignalStore.svr.hasPin(),
      exitWithPin = SignalStore.accessibility.exitWithPin
    )
  }
}
