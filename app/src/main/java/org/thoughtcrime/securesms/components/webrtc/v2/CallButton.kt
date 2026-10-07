/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.webrtc.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.IconButtons
import org.signal.core.ui.compose.NightPreview
import org.signal.core.ui.compose.Previews
import org.thoughtcrime.securesms.R
import org.signal.core.ui.R as CoreUiR

internal val defaultCallButtonIconSize: Dp = 24.dp

@Composable
internal fun ToggleCallButton(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  checkedImageVector: ImageVector = imageVector
) {
  val buttonSize = dimensionResource(id = R.dimen.webrtc_button_size)
  IconButtons.IconToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    size = buttonSize,
    modifier = modifier.size(buttonSize),
    colors = IconButtons.run {
      iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        checkedContentColor = colorResource(id = CoreUiR.color.signal_light_colorOnPrimary),
        containerColor = colorResource(id = CoreUiR.color.signal_light_colorSecondaryContainer),
        contentColor = colorResource(id = CoreUiR.color.signal_light_colorOnSecondaryContainer)
      )
    }
  ) {
    Icon(
      imageVector = if (checked) checkedImageVector else imageVector,
      contentDescription = contentDescription,
      modifier = Modifier.size(28.dp)
    )
  }
}

@Composable
internal fun CallButton(
  onClick: () -> Unit,
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = colorResource(id = CoreUiR.color.signal_light_colorOnPrimary),
  iconSize: Dp = defaultCallButtonIconSize
) {
  val buttonSize = dimensionResource(id = R.dimen.webrtc_button_size)
  IconButtons.IconButton(
    onClick = onClick,
    size = buttonSize,
    modifier = modifier.size(buttonSize),
    colors = IconButtons.iconButtonColors(
      containerColor = containerColor,
      contentColor = contentColor
    )
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      modifier = Modifier.size(iconSize),
      tint = contentColor
    )
  }
}

@Composable
fun ToggleVideoButton(
  isVideoEnabled: Boolean,
  onChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  ToggleCallButton(
    checked = isVideoEnabled,
    onCheckedChange = onChange,
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_video_slash_fill_24),
    checkedImageVector = ImageVector.vectorResource(id = R.drawable.symbol_video_fill_24),
    contentDescription = stringResource(id = R.string.WebRtcCallView__toggle_camera),
    modifier = modifier
  )
}

@Composable
fun ToggleMicButton(
  isMicEnabled: Boolean,
  onChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  ToggleCallButton(
    checked = isMicEnabled,
    onCheckedChange = onChange,
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_mic_slash_fill_24),
    checkedImageVector = ImageVector.vectorResource(id = R.drawable.symbol_mic_fill_white_24),
    contentDescription = stringResource(id = R.string.WebRtcCallView__toggle_mute),
    modifier = modifier
  )
}

@Composable
fun ToggleRingButton(
  isRingEnabled: Boolean,
  isRingAllowed: Boolean,
  onChange: (Boolean, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  ToggleCallButton(
    checked = isRingEnabled,
    onCheckedChange = { onChange(it, isRingAllowed) },
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_bell_slash_fill_24),
    checkedImageVector = ImageVector.vectorResource(id = R.drawable.symbol_bell_ring_fill_white_24),
    contentDescription = stringResource(id = R.string.WebRtcCallView__toggle_group_ringing),
    modifier = modifier
  )
}

@Composable
fun AdditionalActionsButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  CallButton(
    onClick = onClick,
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_more_white_24),
    contentDescription = stringResource(id = R.string.WebRtcCallView__additional_actions),
    modifier = modifier
  )
}

@Composable
fun HangupButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconSize: Dp = defaultCallButtonIconSize
) {
  CallButton(
    onClick = onClick,
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_phone_down_fill_24),
    contentDescription = stringResource(id = R.string.WebRtcCallView__end_call),
    containerColor = colorResource(id = R.color.webrtc_hangup_background),
    modifier = modifier,
    iconSize = iconSize
  )
}

@Composable
fun AcceptCallButton(
  onClick: () -> Unit,
  isVideoCall: Boolean,
  modifier: Modifier = Modifier,
  iconSize: Dp = defaultCallButtonIconSize
) {
  CallButton(
    onClick = onClick,
    imageVector = if (isVideoCall) {
      ImageVector.vectorResource(id = R.drawable.symbol_video_fill_24)
    } else {
      ImageVector.vectorResource(id = R.drawable.symbol_phone_fill_white_24)
    },
    contentDescription = stringResource(id = R.string.WebRtcCallScreen__answer),
    containerColor = colorResource(id = R.color.webrtc_answer_background),
    iconSize = iconSize,
    modifier = modifier
  )
}

@Composable
fun AnswerWithoutVideoButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  CallButton(
    onClick = onClick,
    imageVector = ImageVector.vectorResource(id = R.drawable.symbol_video_slash_fill_24),
    contentDescription = stringResource(id = R.string.WebRtcCallScreen__answer_without_video),
    containerColor = Color.White,
    contentColor = Color.Black,
    modifier = modifier
  )
}

/**
 * Places the call from the lobby.
 *
 * [imageVector] is for Accessibility Mode only: with an icon the button is built exactly like
 * [CallActionButton] -- icon, 12 dp, label -- so that "Start Video Call" and "End Video Call" read
 * as the same kind of control rather than two different ones. Signal's own screens pass nothing and
 * keep the plain label, including its wider padding; an icon there would change a screen this fork
 * has no business changing.
 */
@Composable
fun StartCallButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageVector: ImageVector? = null
) {
  val contentColor = colorResource(id = CoreUiR.color.signal_light_colorOnPrimary)

  Buttons.LargePrimary(
    onClick = onClick,
    modifier = modifier.height(56.dp),
    colors = ButtonDefaults.buttonColors(
      // Grün, entschieden am 5.10.2026, und zwar webrtc_answer_background (#34C759) -- dieselbe
      // Ressource wie AcceptCallButton und der "Answer"-Knopf. Vorher stand hier Signals
      // allgemeines signal_light_colorPrimary; das ist die Farbe für "irgendein Hauptknopf",
      // während dieser hier genau eine Sache tut: einen Anruf beginnen. Gegenstück ist das Rot
      // am CallBackButton.
      containerColor = colorResource(id = R.color.webrtc_answer_background),
      contentColor = contentColor
    ),
    // Mit Symbol dasselbe Innenmaß wie CallActionButton, sonst Signals ursprüngliches.
    contentPadding = if (imageVector != null) {
      PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    } else {
      PaddingValues(horizontal = 48.dp, vertical = 18.dp)
    }
  ) {
    if (imageVector != null) {
      Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(defaultCallButtonIconSize),
        tint = contentColor
      )

      Spacer(modifier = Modifier.width(12.dp))
    }

    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge
    )
  }
}

@NightPreview
@Composable
private fun ToggleMicButtonPreview() {
  Previews.Preview {
    Row {
      ToggleMicButton(
        isMicEnabled = true,
        onChange = {}
      )

      ToggleMicButton(
        isMicEnabled = false,
        onChange = {}
      )
    }
  }
}

@NightPreview
@Composable
private fun ToggleVideoButtonPreview() {
  Previews.Preview {
    Row {
      ToggleVideoButton(
        isVideoEnabled = true,
        onChange = {}
      )

      ToggleVideoButton(
        isVideoEnabled = false,
        onChange = {}
      )
    }
  }
}

@NightPreview
@Composable
private fun ToggleRingButtonPreview() {
  Previews.Preview {
    Row {
      ToggleRingButton(
        isRingEnabled = true,
        isRingAllowed = true,
        onChange = { _, _ -> }
      )

      ToggleRingButton(
        isRingEnabled = false,
        isRingAllowed = true,
        onChange = { _, _ -> }
      )
    }
  }
}

@NightPreview
@Composable
private fun AdditionalActionsButtonPreview() {
  Previews.Preview {
    AdditionalActionsButton(
      onClick = {}
    )
  }
}

@NightPreview
@Composable
private fun HangupButtonPreview() {
  Previews.Preview {
    HangupButton(
      onClick = {}
    )
  }
}

@NightPreview
@Composable
private fun VideoAcceptCallButtonPreview() {
  Previews.Preview {
    AcceptCallButton(
      onClick = {},
      isVideoCall = true
    )
  }
}

@NightPreview
@Composable
private fun AcceptCallButtonPreview() {
  Previews.Preview {
    AcceptCallButton(
      onClick = {},
      isVideoCall = false
    )
  }
}

@NightPreview
@Composable
private fun AnswerWithoutVideoButtonPreview() {
  Previews.Preview {
    AnswerWithoutVideoButton(
      onClick = {}
    )
  }
}

@NightPreview
@Composable
private fun StartCallButtonPreview() {
  Previews.Preview {
    StartCallButton(
      stringResource(id = R.string.WebRtcCallView__start_call),
      onClick = {}
    )
  }
}

@NightPreview
@Composable
private fun JoinCallButtonPreview() {
  Previews.Preview {
    StartCallButton(
      stringResource(id = R.string.WebRtcCallView__join_call),
      onClick = {}
    )
  }
}
