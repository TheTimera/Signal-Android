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
      // Both setters bound the PAIR, not just their own value. Ten taps and a one second limit are
      // each within their own bounds and together impossible, and with the PIN switched off that
      // is a permanent lockout. The list no longer offers such a pair; this is the second line of
      // defence, for values a previous build may have stored.
      is AccessibilityExitSettingsEvents.SetTapCount -> {
        val count = event.count.coerceIn(AccessibilityValues.MIN_TAP_COUNT, AccessibilityValues.MAX_TAP_COUNT)
        val window = SignalStore.accessibility.tapWindowMillis
          .coerceAtLeast(AccessibilityValues.minTapWindowMillisFor(count))
        SignalStore.accessibility.tapCount = count
        SignalStore.accessibility.tapWindowMillis = window
        _state.value = _state.value.copy(tapCount = count, tapWindowMillis = window)
      }
      is AccessibilityExitSettingsEvents.SetTapWindowMillis -> {
        val window = event.millis.coerceIn(AccessibilityValues.MIN_TAP_WINDOW_MILLIS, AccessibilityValues.MAX_TAP_WINDOW_MILLIS)
        val count = SignalStore.accessibility.tapCount
          .coerceAtMost(AccessibilityValues.maxTapCountFor(window))
        SignalStore.accessibility.tapWindowMillis = window
        SignalStore.accessibility.tapCount = count
        _state.value = _state.value.copy(tapWindowMillis = window, tapCount = count)
      }
    }
  }

  private fun loadState(): AccessibilityExitSettingsState {
    val count = SignalStore.accessibility.tapCount
      .coerceIn(AccessibilityValues.MIN_TAP_COUNT, AccessibilityValues.MAX_TAP_COUNT)
    val window = SignalStore.accessibility.tapWindowMillis
      .coerceIn(AccessibilityValues.MIN_TAP_WINDOW_MILLIS, AccessibilityValues.MAX_TAP_WINDOW_MILLIS)
      .coerceAtLeast(AccessibilityValues.minTapWindowMillisFor(count))

    // Written back, not merely displayed. Showing a corrected window while the gesture still
    // evaluates the stored one would be the worse of the two failures: the screen would describe a
    // way out that does not work.
    if (count != SignalStore.accessibility.tapCount) SignalStore.accessibility.tapCount = count
    if (window != SignalStore.accessibility.tapWindowMillis) SignalStore.accessibility.tapWindowMillis = window

    return AccessibilityExitSettingsState(
      exitWithPin = SignalStore.accessibility.exitWithPin,
      hasPin = SignalStore.svr.hasPin(),
      tapCorner = SignalStore.accessibility.tapCorner,
      tapCount = count,
      tapWindowMillis = window
    )
  }
}
