/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.keyvalue

import org.signal.core.util.LongSerializer

/**
 * Where on screen the exit taps have to land when the tap method is used to leave Accessibility Mode.
 */
enum class AccessibilityTapCorner(private val code: Long) {
  TOP_LEFT(0),
  TOP_RIGHT(1),
  BOTTOM_LEFT(2),
  BOTTOM_RIGHT(3);

  fun serialize(): Long {
    return code
  }

  companion object Serializer : LongSerializer<AccessibilityTapCorner> {
    override fun serialize(data: AccessibilityTapCorner): Long {
      return data.serialize()
    }

    /**
     * Falls back rather than throwing: a corrupted preference should not be able to crash a device
     * whose user cannot leave Accessibility Mode without it.
     */
    override fun deserialize(data: Long): AccessibilityTapCorner {
      return entries.firstOrNull { it.code == data } ?: TOP_RIGHT
    }
  }
}

/**
 * Settings for Accessibility Mode: a stripped-down, single-purpose video calling surface for people
 * who are overwhelmed by the full app. A caregiver sets it up on the user's device and then locks it,
 * either behind the Signal PIN or behind a tap gesture.
 *
 * Defaults are taken from the design, not chosen freely:
 *  - [isVideoCallOnly] is not stored at all, see the property for why.
 *  - [exitWithPin] on, because the design states "Use Signal-PIN (default if PIN is set)".
 *  - [iconsAlwaysVisible] on and the three exception toggles off, as shown on design screen 130.
 *  - [tapCount] 7, [tapWindowMillis] 2000 and [tapCorner] top right, as labelled in the design.
 */
class AccessibilityValues(store: KeyValueStore) : SignalStoreValues(store) {

  companion object {
    const val ENABLED = "accessibility.enabled"
    const val ICONS_ALWAYS_VISIBLE = "accessibility.icons_always_visible"
    const val ALLOW_CAMERA_TOGGLE = "accessibility.allow_camera_toggle"
    const val ALLOW_MIC_TOGGLE = "accessibility.allow_mic_toggle"
    const val ALLOW_CAMERA_SWITCH = "accessibility.allow_camera_switch"
    const val EXIT_WITH_PIN = "accessibility.exit_with_pin"
    const val TAP_COUNT = "accessibility.tap_count"
    const val TAP_WINDOW_MILLIS = "accessibility.tap_window_millis"
    const val TAP_CORNER = "accessibility.tap_corner"

    /**
     * The design warns: "Avoid setting the time limit too short, as users may not be able to tap
     * quickly enough." Too short a window would lock the caregiver out of their own device.
     */
    const val MIN_TAP_WINDOW_MILLIS = 1000
    const val MAX_TAP_WINDOW_MILLIS = 10000
    const val MIN_TAP_COUNT = 3
    const val MAX_TAP_COUNT = 15
  }

  var isEnabled: Boolean by booleanValue(ENABLED, false)

  var iconsAlwaysVisible: Boolean by booleanValue(ICONS_ALWAYS_VISIBLE, true)

  /**
   * Derived rather than stored. The design's settings screen has no "Video-Call only" switch of its
   * own; it says "The default setting is 'Video-Call Only,' but you can allow users to toggle the
   * camera and microphone on and off". The restriction therefore holds exactly as long as no
   * exception has been granted. A separate stored flag could contradict the three below.
   */
  val isVideoCallOnly: Boolean
    get() = !allowCameraToggle && !allowMicToggle && !allowCameraSwitch

  /*
   * The questions the call screen actually asks. Each answers "yes" whenever the mode is off, so
   * call sites can ask unconditionally instead of repeating the [isEnabled] check -- that repeated
   * check is what would drift apart over time. Named differently from the stored flags above on
   * purpose: "may…" is the policy, "allow…" is what the caregiver ticked.
   */

  val mayToggleCamera: Boolean
    get() = !isEnabled || allowCameraToggle

  val mayToggleMic: Boolean
    get() = !isEnabled || allowMicToggle

  val maySwitchCamera: Boolean
    get() = !isEnabled || allowCameraSwitch

  val forcesControlsVisible: Boolean
    get() = isEnabled && iconsAlwaysVisible

  /**
   * Whether to hold back popups nobody asked for -- app rating, PIN reminder and the other
   * megaphones, relink and restore sheets, battery saver and debug log prompts. The mode's screen
   * fills the window and offers no way to dismiss a dialog, so one landing on top of it strands the
   * user. Same shape as the questions above: asks unconditionally, answers "no" while the mode is
   * off. Whatever set the popup's flag keeps it set, so it is deferred and not swallowed.
   */
  val suppressesPopups: Boolean
    get() = isEnabled

  var allowCameraToggle: Boolean by booleanValue(ALLOW_CAMERA_TOGGLE, false)

  var allowMicToggle: Boolean by booleanValue(ALLOW_MIC_TOGGLE, false)

  var allowCameraSwitch: Boolean by booleanValue(ALLOW_CAMERA_SWITCH, false)

  var exitWithPin: Boolean by booleanValue(EXIT_WITH_PIN, true)

  var tapCount: Int by integerValue(TAP_COUNT, 7)

  var tapWindowMillis: Int by integerValue(TAP_WINDOW_MILLIS, 2000)

  var tapCorner: AccessibilityTapCorner by enumValue(TAP_CORNER, AccessibilityTapCorner.TOP_RIGHT, AccessibilityTapCorner.Serializer)

  public override fun onFirstEverAppLaunch() = Unit

  /**
   * Deliberately empty: these settings describe one physical device that a caregiver configured, so
   * restoring them onto a different device would silently lock that device down.
   */
  public override fun getKeysToIncludeInBackup(): List<String> = emptyList()
}
