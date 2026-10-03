/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import org.thoughtcrime.securesms.keyvalue.AccessibilityTapCorner

sealed interface AccessibilityExitSettingsEvents {
  data class ToggleExitWithPin(val enabled: Boolean) : AccessibilityExitSettingsEvents
  data class SetTapCorner(val corner: AccessibilityTapCorner) : AccessibilityExitSettingsEvents
  data class SetTapCount(val count: Int) : AccessibilityExitSettingsEvents
  data class SetTapWindowMillis(val millis: Int) : AccessibilityExitSettingsEvents
}
