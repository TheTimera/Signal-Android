/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.webrtc.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.IconButtons
import org.signal.core.ui.compose.SignalIcons
import org.thoughtcrime.securesms.R
import org.signal.core.ui.R as CoreUiR

/**
 * A call action as a labelled pill: symbol first, words after it, inside the button.
 *
 * Accessibility Mode uses this wherever Signal would use a bare circle with a caption underneath. An
 * icon alone asks the user to know that a dropped handset means "end call"; a caption underneath
 * asks them to connect two things that are drawn apart. One button, one meaning.
 */
@Composable
fun CallActionButton(
  text: String,
  imageVector: ImageVector,
  containerColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentColor: Color = Color.White
) {
  Buttons.LargePrimary(
    onClick = onClick,
    modifier = modifier.height(56.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = null,
      modifier = Modifier.size(defaultCallButtonIconSize),
      tint = contentColor
    )

    Spacer(modifier = Modifier.width(12.dp))

    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge
    )
  }
}

/**
 * Back out of the lobby, as a round tonal button in the control strip. Same tokens the mode uses for
 * going back elsewhere, so it reads as the same action in a different place.
 */
@Composable
fun CallBackButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val buttonSize = dimensionResource(id = R.dimen.webrtc_button_size)

  IconButtons.IconButton(
    onClick = onClick,
    size = buttonSize,
    modifier = modifier.size(buttonSize),
    colors = IconButtons.iconButtonColors(
      // Red on purpose: the lobby offers two ways out side by side, place the call or do not.
      // Signal colours that pair the same way everywhere -- green to accept, red to decline and to
      // hang up -- and the lobby is the same decision one step earlier. These are Signal's own
      // resources rather than a custom shade: webrtc_hangup_background (#F07168), the one
      // EndVideoCallButton, HangupButton and "Decline" already use.
      containerColor = colorResource(id = R.color.webrtc_hangup_background),
      contentColor = colorResource(id = CoreUiR.color.signal_light_colorOnPrimary)
    )
  ) {
    Icon(
      imageVector = SignalIcons.ArrowStart.imageVector,
      contentDescription = stringResource(id = R.string.CallScreenTopBar__go_back),
      modifier = Modifier.size(defaultCallButtonIconSize)
    )
  }
}

/** Hang up, as a labelled pill. Same shape and height as [StartCallButton]. */
@Composable
fun EndVideoCallButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  CallActionButton(
    text = stringResource(id = R.string.Accessibility__end_video_call),
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_phone_down_fill_24),
    containerColor = colorResource(id = R.color.webrtc_hangup_background),
    onClick = onClick,
    modifier = modifier
  )
}

/**
 * Screen sharing from the control strip, for Accessibility Mode -- the mode has no overflow menu to
 * hide it in. Checked while sharing, so the same button stops it again.
 */
@Composable
fun ToggleScreenShareButton(
  isScreenSharing: Boolean,
  onChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  // Inverted on purpose. Signal's toggle buttons draw the *unchecked* state light and the checked
  // one dark, because the state worth noticing is "your microphone is muted". For screen sharing the
  // state worth noticing is the opposite one -- that you are sharing -- so passing the flag straight
  // through would light the button up while nothing is being shared.
  ToggleCallButton(
    checked = !isScreenSharing,
    onCheckedChange = { onChange(!it) },
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_screen_share_24),
    contentDescription = stringResource(id = R.string.CallOverflowPopupWindow__share_screen),
    modifier = modifier
  )
}

/**
 * Switches between front and rear camera from the control strip. Signal's own switch sits in the
 * corner of the small self preview, which is too small a target for Accessibility Mode.
 */
@Composable
fun SwitchCameraDirectionButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  CallButton(
    onClick = onClick,
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_switch_24),
    contentDescription = stringResource(id = R.string.SwitchCameraButton__switch_camera_direction),
    modifier = modifier
  )
}
