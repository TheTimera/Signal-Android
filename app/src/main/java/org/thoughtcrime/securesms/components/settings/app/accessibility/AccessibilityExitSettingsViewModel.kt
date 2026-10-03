/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.thoughtcrime.securesms.keyvalue.AccessibilityValues
import org.thoughtcrime.securesms.keyvalue.SignalStore

class AccessibilityExitSettingsViewModel : ViewModel() {

  private val _state = MutableStateFlow(loadState())
  val state: StateFlow<AccessibilityExitSettingsState> = _state

  fun onEvent(event: AccessibilityExitSettingsEvents) {
    when (event) {
      is AccessibilityExitSettingsEvents.ToggleExitWithPin -> {
        SignalStore.accessibility.exitWithPin = event.enabled
        _state.value = _state.value.copy(exitWithPin = event.enabled)
      }
      is AccessibilityExitSettingsEvents.SetTapCorner -> {
        SignalStore.accessibility.tapCorner = event.corner
        _state.value = _state.value.copy(tapCorner = event.corner)
      }
      is AccessibilityExitSettingsEvents.SetTapCount -> {
        val clamped = event.count.coerceIn(AccessibilityValues.MIN_TAP_COUNT, AccessibilityValues.MAX_TAP_COUNT)
        SignalStore.accessibility.tapCount = clamped
        _state.value = _state.value.copy(tapCount = clamped)
      }
      is AccessibilityExitSettingsEvents.SetTapWindowMillis -> {
        // The design warns against a window so short that the caregiver cannot tap fast enough --
        // that would lock them out of their own device, so the bound is enforced here too.
        val clamped = event.millis.coerceIn(AccessibilityValues.MIN_TAP_WINDOW_MILLIS, AccessibilityValues.MAX_TAP_WINDOW_MILLIS)
        SignalStore.accessibility.tapWindowMillis = clamped
        _state.value = _state.value.copy(tapWindowMillis = clamped)
      }
    }
  }

  private fun loadState(): AccessibilityExitSettingsState {
    return AccessibilityExitSettingsState(
      exitWithPin = SignalStore.accessibility.exitWithPin,
      hasPin = SignalStore.svr.hasPin(),
      tapCorner = SignalStore.accessibility.tapCorner,
      tapCount = SignalStore.accessibility.tapCount,
      tapWindowMillis = SignalStore.accessibility.tapWindowMillis
    )
  }
}
