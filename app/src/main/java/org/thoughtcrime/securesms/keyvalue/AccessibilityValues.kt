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
 *  - [exitWithPin] on, because the design states "Use Signal-PIN (default if PIN is set)".
 *  - [iconsAlwaysVisible] on and the exception toggles off, as shown on design screen 130: "The
 *    default setting is Video-Call Only, but you can allow users to toggle the camera and
 *    microphone on and off."
 *  - [tapCount] 7, [tapWindowMillis] 2000 and [tapCorner] top right, as labelled in the design.
 *
 * [allowScreenShare] has no counterpart in the design; it follows the same shape as the three it
 * was added next to.
 */
class AccessibilityValues(store: KeyValueStore) : SignalStoreValues(store) {

  companion object {
    const val ENABLED = "accessibility.enabled"
    const val ICONS_ALWAYS_VISIBLE = "accessibility.icons_always_visible"
    const val ALLOW_CAMERA_TOGGLE = "accessibility.allow_camera_toggle"
    const val ALLOW_MIC_TOGGLE = "accessibility.allow_mic_toggle"
    const val ALLOW_CAMERA_SWITCH = "accessibility.allow_camera_switch"
    const val ALLOW_SCREEN_SHARE = "accessibility.allow_screen_share"
    const val ALLOW_ANSWER_WITHOUT_VIDEO = "accessibility.allow_answer_without_video"
    const val SPEAKER_ALWAYS_ON = "accessibility.speaker_always_on"
    const val SHOW_MIC_LEVEL = "accessibility.show_mic_level"
    const val HIDE_MESSAGE_NOTIFICATIONS = "accessibility.hide_message_notifications"
    const val CALL_START_VOLUME_PERCENT = "accessibility.call_start_volume_percent"
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

    /**
     * Fastest tapping that counts as doable by hand. Bounding each value on its own is not enough:
     * ten taps and a one second limit are both individually fine and together impossible, and with
     * the Signal PIN switched off that combination is a permanent lockout -- the app's own info
     * page can then only offer resetting Signal. So the pair is bounded, in one place, and the
     * settings list offers nothing that fails [isTapPatternReachable].
     *
     * Four per second rather than a stricter figure because the shipped default is seven taps in
     * two seconds, or 3.5 per second, and that default is in daily use. The gesture is also made
     * by whoever maintains the device, not by the person the mode is set up for.
     */
    const val MAX_TAPS_PER_SECOND = 4

    fun isTapPatternReachable(count: Int, windowMillis: Int): Boolean {
      return count * 1000 <= windowMillis * MAX_TAPS_PER_SECOND
    }

    /** Shortest window in which [count] taps are still doable, rounded up to whole seconds. */
    fun minTapWindowMillisFor(count: Int): Int {
      val needed = ((count + MAX_TAPS_PER_SECOND - 1) / MAX_TAPS_PER_SECOND) * 1000
      return needed.coerceIn(MIN_TAP_WINDOW_MILLIS, MAX_TAP_WINDOW_MILLIS)
    }

    /** Most taps that are still doable within [windowMillis]. */
    fun maxTapCountFor(windowMillis: Int): Int {
      val possible = (windowMillis / 1000) * MAX_TAPS_PER_SECOND
      return possible.coerceIn(MIN_TAP_COUNT, MAX_TAP_COUNT)
    }
  }

  var isEnabled: Boolean by booleanValue(ENABLED, false)

  var iconsAlwaysVisible: Boolean by booleanValue(ICONS_ALWAYS_VISIBLE, true)

  var allowCameraToggle: Boolean by booleanValue(ALLOW_CAMERA_TOGGLE, false)

  var allowMicToggle: Boolean by booleanValue(ALLOW_MIC_TOGGLE, false)

  var allowCameraSwitch: Boolean by booleanValue(ALLOW_CAMERA_SWITCH, false)

  var allowScreenShare: Boolean by booleanValue(ALLOW_SCREEN_SHARE, false)

  /**
   * Whether an incoming call can be answered with the camera off.
   *
   * Off by default since 5.10.2026, decided by the person this fork is built for. The rule it now
   * follows is the plain one: the mode starts with two buttons, decline and answer, and every
   * switch on the settings page adds a third thing to look at. The earlier default was on, argued
   * from "a call you cannot take without video is a call you may not take at all" -- that argument
   * is not wrong, but it belongs to whoever sets the device up, not to the shipped state.
   */
  var allowAnswerWithoutVideo: Boolean by booleanValue(ALLOW_ANSWER_WITHOUT_VIDEO, false)

  /**
   * Whether every call is routed to the speakerphone on connect. On by default: a tablet held at
   * arm's length for a video call is not held against an ear, and the earpiece would be inaudible.
   *
   * ⚠️ This also overrides a connected headset. On the target device there is none; on a device that
   * has one, this switch is the wrong one to leave on.
   */
  var speakerAlwaysOn: Boolean by booleanValue(SPEAKER_ALWAYS_ON, true)

  /** Off by default: one more moving thing on the call screen, and nothing the user has to act on. */
  var showMicLevel: Boolean by booleanValue(SHOW_MIC_LEVEL, false)

  /**
   * Whether message notifications are withheld while the mode is on. Default on, and the one
   * switch here whose off position reopens a hole rather than merely adding a button: a message
   * notification is a door into the full app that goes around the PIN or tap pattern.
   */
  var hideMessageNotifications: Boolean by booleanValue(HIDE_MESSAGE_NOTIFICATIONS, true)

  /**
   * How loud a call starts, as a percentage of the device's own call volume scale. Stored as a
   * percentage rather than a volume index because that scale differs per device -- 5 steps here, 15
   * on the next phone -- and a stored index would mean something else on each.
   *
   * 80 by default: the mode exists for people who would not find the volume rocker, so starting
   * quiet and hoping they fix it is the wrong way round.
   */
  var callStartVolumePercent: Int by integerValue(CALL_START_VOLUME_PERCENT, 80)

  /**
   * The volume index to set when a call connects, on a scale that runs from [deviceMin] to
   * [deviceMax].
   *
   * Never returns the bottom of the scale when that would be silence: the whole point is that a call
   * is never started muted. Hence the coerce to at least one step above a zero minimum.
   */
  fun callStartVolumeIndex(deviceMin: Int, deviceMax: Int): Int {
    val span = deviceMax - deviceMin
    val raw = deviceMin + Math.round(span * callStartVolumePercent / 100f)

    return raw.coerceIn(maxOf(deviceMin, 1), deviceMax)
  }

  val forcesControlsVisible: Boolean
    get() = isEnabled && iconsAlwaysVisible

  /*
   * The questions the call screen actually asks. Each answers "yes" whenever the mode is off, so
   * call sites can ask unconditionally instead of repeating the [isEnabled] check -- that repeated
   * check is what would drift apart over time. Named differently from the stored flags above on
   * purpose: "may..." is the policy, "allow..." is what the caregiver ticked.
   */

  val mayToggleCamera: Boolean
    get() = !isEnabled || allowCameraToggle

  val mayToggleMic: Boolean
    get() = !isEnabled || allowMicToggle

  val maySwitchCamera: Boolean
    get() = !isEnabled || allowCameraSwitch

  /**
   * Outside the mode this answers "yes" like its neighbours, but Signal's own screen share entry
   * sits behind the overflow menu and its own remote flag -- so nothing changes there.
   */
  val mayShareScreen: Boolean
    get() = !isEnabled || allowScreenShare

  val mayAnswerWithoutVideo: Boolean
    get() = !isEnabled || allowAnswerWithoutVideo

  val forcesSpeakerphone: Boolean
    get() = isEnabled && speakerAlwaysOn

  /**
   * Whether the microphone level is drawn at all. Only asked inside the mode -- outside it Signal
   * draws its own indicators, which this fork does not touch.
   */
  val showsMicLevel: Boolean
    get() = !isEnabled || showMicLevel

  /**
   * Whether [org.thoughtcrime.securesms.notifications.v2.DefaultMessageNotifier] should post
   * nothing. Calls do not pass through that notifier, so this never silences a ringing phone.
   */
  val suppressesMessageNotifications: Boolean
    get() = isEnabled && hideMessageNotifications

  /**
   * Whether to hold back popups nobody asked for -- app rating, PIN reminder and the other
   * megaphones, relink and restore sheets, battery saver and debug log prompts. The mode's screen
   * fills the window and offers no way to dismiss a dialog, so one landing on top of it strands the
   * user. Same shape as the questions above: asks unconditionally, answers "no" while the mode is
   * off. Whatever set the popup's flag keeps it set, so it is deferred and not swallowed.
   */
  val suppressesPopups: Boolean
    get() = isEnabled

  /**
   * Whether Signal's call screen drops the chrome the mode has no use for: the call info button, the
   * draggable sheet that reveals it, and the plain back arrow, which becomes the same round tonal
   * button the mode uses elsewhere. Phrased as one question rather than four, because they are one
   * decision -- a screen with exactly two things on it, the person and the way to call them.
   */
  val simplifiesCallScreen: Boolean
    get() = isEnabled

  var exitWithPin: Boolean by booleanValue(EXIT_WITH_PIN, true)

  var tapCount: Int by integerValue(TAP_COUNT, 7)

  var tapWindowMillis: Int by integerValue(TAP_WINDOW_MILLIS, 2000)

  var tapCorner: AccessibilityTapCorner by enumValue(TAP_CORNER, AccessibilityTapCorner.TOP_RIGHT, AccessibilityTapCorner.Serializer)

  /**
   * What the gesture is actually measured against. The settings screen also corrects an impossible
   * pair when it loads, but the gesture must not depend on anyone having opened that screen: these
   * two are read on every start of the mode, and a pair stored by an earlier build would otherwise
   * stay in force -- as a way out that cannot be performed.
   */
  val effectiveTapWindowMillis: Int
    get() = tapWindowMillis
      .coerceIn(MIN_TAP_WINDOW_MILLIS, MAX_TAP_WINDOW_MILLIS)
      .coerceAtLeast(minTapWindowMillisFor(tapCount.coerceIn(MIN_TAP_COUNT, MAX_TAP_COUNT)))

  val effectiveTapCount: Int
    get() = tapCount.coerceIn(MIN_TAP_COUNT, MAX_TAP_COUNT)

  public override fun onFirstEverAppLaunch() = Unit

  /**
   * Deliberately empty: these settings describe one physical device that a caregiver configured, so
   * restoring them onto a different device would silently lock that device down.
   */
  public override fun getKeysToIncludeInBackup(): List<String> = emptyList()
}
