/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.accessibility

import androidx.compose.runtime.Immutable
import org.thoughtcrime.securesms.conversation.colors.AvatarColor
import org.thoughtcrime.securesms.recipients.RecipientId

/**
 * One tile on the Accessibility Mode home screen. Only plain values are carried across, so the
 * state stays comparable; the tile draws a lettered circle from them rather than loading a photo.
 */
@Immutable
data class AccessibilityContact(
  val id: RecipientId,
  val name: String,
  val color: AvatarColor
)

/**
 * The call that just ended, while it is still worth saying so.
 *
 * [endedAtMillis] rather than a countdown: the screen can be left and come back, and the notice has
 * to disappear on time either way.
 */
@Immutable
data class AccessibilityEndedCall(
  val durationMillis: Long,
  val endedAtMillis: Long
)

@Immutable
data class AccessibilityHomeState(
  val loading: Boolean = true,
  val contacts: List<AccessibilityContact> = emptyList(),
  val endedCall: AccessibilityEndedCall? = null
)
