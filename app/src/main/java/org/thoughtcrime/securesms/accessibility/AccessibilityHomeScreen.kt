/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.accessibility

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.signal.core.ui.compose.Dialogs
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.statusBarsCompat
import org.signal.core.util.EllapsedTimeFormatter
import org.thoughtcrime.securesms.avatar.AvatarImage
import org.thoughtcrime.securesms.keyvalue.AccessibilityTapCorner
import org.thoughtcrime.securesms.recipients.RecipientId

private val TILE_WIDTH = 280.dp

/** Reserved for the "call ended" notice, so the tiles do not move when it comes and goes. */
private val NOTICE_HEIGHT = 108.dp

/** How big the invisible exit target is. Large enough to hit, small enough to miss by accident. */
private val EXIT_TARGET_SIZE = 84.dp

/**
 * The whole app while Accessibility Mode is on: one tile per contact, and tapping one goes straight
 * into Signal's own call lobby. The lobby already shows who is about to be called and asks for a
 * second tap on "Start Video Call", so a confirmation screen of our own sat in front of it saying
 * the same thing twice.
 *
 * Exactly one way back out is offered, matching the configured method:
 *  - PIN method: an overflow button in the top right, which asks for the Signal PIN.
 *  - Tap method: nothing visible at all, just the hidden tap pattern followed by a confirmation.
 *
 * Showing both at once would defeat the point -- the tap pattern is only worth anything while it is
 * the *only* way out, and a visible button next to it would hand the device back to anyone.
 */
@Composable
fun AccessibilityHomeScreen(
  state: AccessibilityHomeState,
  pinExitEnabled: Boolean,
  exitCorner: AccessibilityTapCorner,
  exitTapCount: Int,
  exitTapWindowMillis: Int,
  onCallClick: (RecipientId) -> Unit,
  onExit: () -> Unit,
  onEndedCallExpired: () -> Unit
) {
  var showPinDialog by remember { mutableStateOf(false) }
  var showTapConfirmation by remember { mutableStateOf(false) }

  // Swallows the system back gesture: this is the only screen the mode has, leaving the app is what
  // the configured exit method is for, and a stray back press must not drop the user onto the
  // launcher with no way of finding Signal again.
  BackHandler { }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface
  ) {
    ContactPicker(
      state = state,
      pinExitEnabled = pinExitEnabled,
      exitCorner = exitCorner,
      exitTapCount = exitTapCount,
      exitTapWindowMillis = exitTapWindowMillis,
      onContactSelected = { onCallClick(it.id) },
      onOverflowClick = { showPinDialog = true },
      onTapPatternCompleted = { showTapConfirmation = true },
      onEndedCallExpired = onEndedCallExpired
    )
  }

  if (showPinDialog) {
    AccessibilityExitDialog(
      onDismiss = { showPinDialog = false },
      onLeave = {
        showPinDialog = false
        onExit()
      }
    )
  }

  if (showTapConfirmation) {
    Dialogs.SimpleAlertDialog(
      title = "Leave Accessibility Mode?",
      body = "The full Signal app comes back. You can switch Accessibility Mode on again in Settings.",
      confirm = "Yes",
      dismiss = "No",
      onConfirm = {
        showTapConfirmation = false
        onExit()
      },
      onDismiss = { showTapConfirmation = false }
    )
  }
}

/**
 * "Video-Call ended" and how long it lasted, for [ENDED_CALL_NOTICE_MILLIS] after hanging up.
 *
 * Holds its height even when there is nothing to say. The alternative -- appearing and disappearing
 * above the tiles -- would move the tiles twice per call, and this screen's whole point is that the
 * target stays where it was.
 */
@Composable
private fun EndedCallNotice(
  endedCall: AccessibilityEndedCall?,
  onExpired: () -> Unit
) {
  val remaining = endedCall?.let { ENDED_CALL_NOTICE_MILLIS - (System.currentTimeMillis() - it.endedAtMillis) }

  LaunchedEffect(endedCall) {
    if (endedCall != null) {
      delay(remaining?.coerceAtLeast(0L) ?: 0L)
      onExpired()
    }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.height(NOTICE_HEIGHT)
  ) {
    if (endedCall != null && (remaining ?: 0L) > 0L) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Video-Call ended",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.error
        )

        Text(
          // Seconds, despite the name: fromDurationMillis divides by 3600 and 60, and Signal's own
          // call screen feeds it the elapsed seconds. Passing millis showed 14:27:18 for a 52
          // second call.
          text = EllapsedTimeFormatter.fromDurationMillis(endedCall.durationMillis / 1000)?.toString() ?: "00:00",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun ContactPicker(
  state: AccessibilityHomeState,
  pinExitEnabled: Boolean,
  exitCorner: AccessibilityTapCorner,
  exitTapCount: Int,
  exitTapWindowMillis: Int,
  onContactSelected: (AccessibilityContact) -> Unit,
  onOverflowClick: () -> Unit,
  onTapPatternCompleted: () -> Unit,
  onEndedCallExpired: () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    when {
      state.loading -> {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }

      state.contacts.isEmpty() -> {
        Text(
          text = "No contacts yet. Whoever set up this device needs to start a chat first.",
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .align(Alignment.Center)
            .padding(48.dp)
        )
      }

      else -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.align(Alignment.Center)
        ) {
          // Laid out above the tiles but without moving them: the notice and its placeholder are
          // the same height, so a tile never shifts under a finger that is already on its way.
          EndedCallNotice(
            endedCall = state.endedCall,
            onExpired = onEndedCallExpired
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            state.contacts.forEach { contact ->
              ContactTile(
                contact = contact,
                onClick = { onContactSelected(contact) },
                modifier = Modifier.width(TILE_WIDTH)
              )
            }
          }
        }
      }
    }

    if (pinExitEnabled) {
      IconButton(
        onClick = onOverflowClick,
        modifier = Modifier
          .align(Alignment.TopEnd)
          // Without the inset the button sits inside the status bar; the extra padding then keeps
          // it clear of the screen edges rather than hugging the corner.
          .windowInsetsPadding(WindowInsets.statusBarsCompat)
          .padding(top = 24.dp, end = 24.dp)
      ) {
        Icon(
          imageVector = SignalIcons.MoreVertical.imageVector,
          contentDescription = "Menu",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      ExitTapTarget(
        tapCount = exitTapCount,
        tapWindowMillis = exitTapWindowMillis,
        onCompleted = onTapPatternCompleted,
        modifier = Modifier.align(
          when (exitCorner) {
            AccessibilityTapCorner.TOP_LEFT -> Alignment.TopStart
            AccessibilityTapCorner.TOP_RIGHT -> Alignment.TopEnd
            AccessibilityTapCorner.BOTTOM_LEFT -> Alignment.BottomStart
            AccessibilityTapCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
          }
        )
      )
    }
  }
}

/**
 * The whole tile is the button. Accessibility Mode only ever starts a video call, so a separate
 * control would just be a smaller target for the same single action.
 */
@Composable
private fun ContactTile(
  contact: AccessibilityContact,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
      AvatarImage(
        recipientId = contact.id,
        modifier = Modifier
          .size(168.dp)
          .clip(CircleShape)
      )

      Text(
        text = contact.name,
        style = MaterialTheme.typography.headlineSmall,
        fontSize = 28.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp)
      )
    }
  }
}

/**
 * Invisible and unlabelled on purpose: the person using the device should not discover it, while the
 * caregiver knows where it is because they chose the corner themselves.
 */
@Composable
private fun ExitTapTarget(
  tapCount: Int,
  tapWindowMillis: Int,
  onCompleted: () -> Unit,
  modifier: Modifier = Modifier
) {
  var tapTimes by remember(tapCount, tapWindowMillis) { mutableStateOf(emptyList<Long>()) }
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .size(EXIT_TARGET_SIZE)
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) {
        // elapsedRealtime rather than currentTimeMillis: a clock change must not open the lock.
        val now = SystemClock.elapsedRealtime()
        val recent = (tapTimes + now).filter { now - it <= tapWindowMillis }

        if (recent.size >= tapCount) {
          tapTimes = emptyList()
          onCompleted()
        } else {
          tapTimes = recent
        }
      }
  )
}
