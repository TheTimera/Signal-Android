/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.webrtc.v2

import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.signal.core.ui.compose.IconButtons
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.events.CallParticipant
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.signal.core.ui.R as CoreUiR

private const val POLL_MILLIS = 1000L

/** Fixed room for the microphone level, which comes and goes on its own. */
private val MIC_SLOT_SIZE = 52.dp

/**
 * Volume up and down for the call itself, as a pair at the screen edge, with the current level
 * between them.
 *
 * Accessibility Mode cannot assume its user finds the volume rocker on the side of a tablet, so the
 * same adjustment is offered on screen. It moves STREAM_VOICE_CALL -- the stream
 * [WebRtcCallActivity] already hands the hardware keys -- so both ways end up on the same scale.
 *
 * Three decisions worth keeping:
 *  - The level is SET to an explicit index rather than nudged with adjustStreamVolume, and read back
 *    afterwards. If the system refuses the change, the bar does not move, which is the truth rather
 *    than a button that pretends.
 *  - No FLAG_SHOW_UI: Android's own volume panel is exactly the popup this mode works to avoid.
 *  - Both buttons keep the same colour at the ends of the scale instead of greying out. A disabled
 *    button reads as broken; the bar next to it already says why nothing moves.
 */
@Composable
fun CallVolumeControls(
  localParticipant: CallParticipant? = null,
  showVolume: Boolean = true,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val audioManager = remember { ContextCompat.getSystemService(context, AudioManager::class.java) } ?: return

  val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) }
  val minVolume = remember {
    if (Build.VERSION.SDK_INT >= 28) audioManager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL) else 0
  }

  var volume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)) }

  // The hardware keys move the same stream, so the bar has to follow changes it did not cause.
  LaunchedEffect(Unit) {
    while (true) {
      delay(POLL_MILLIS)
      volume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
    }
  }

  fun setVolume(target: Int) {
    val clamped = target.coerceIn(minVolume, maxVolume)
    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, clamped, 0)
    volume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
  }

  Column(
    verticalArrangement = spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    // ⚠️ Vor dem Anruf wird dieser Teil nur UNSICHTBAR geschaltet, nicht weggelassen. Er hält dann
    //    seinen Platz, und damit steht die Mikrofonanzeige darunter in der Lobby pixelgenau dort,
    //    wo sie auch im Gespräch steht -- genau das war die Anforderung. Würde man die Knöpfe
    //    weglassen, rutschte die Anzeige nach oben und wäre vor und während des Anrufs an zwei
    //    verschiedenen Stellen.
    val sichtbar = Modifier.alpha(if (showVolume) 1f else 0f)

    Box(modifier = sichtbar) {
      VolumeLevel(
        volume = volume,
        minVolume = minVolume,
        maxVolume = maxVolume
      )
    }

    Box(modifier = sichtbar) {
      VolumeButton(
        imageVector = ImageVector.vectorResource(id = R.drawable.symbol_plus_circle_24),
        contentDescription = if (showVolume) stringResource(R.string.Accessibility__volume_up) else "",
        // Unsichtbar heißt auch unbedienbar: alpha allein nimmt die Berührfläche nicht weg.
        onClick = { if (showVolume) setVolume(volume + 1) }
      )
    }

    Box(modifier = sichtbar) {
      VolumeButton(
        imageVector = ImageVector.vectorResource(id = R.drawable.accessibility_minus_circle_24),
        contentDescription = if (showVolume) stringResource(R.string.Accessibility__volume_down) else "",
        onClick = { if (showVolume) setVolume(volume - 1) }
      )
    }

    // The microphone level belongs next to the thing it is read against -- how loud the other side
    // is -- not in the opposite corner, which is where Signal puts it.
    //
    // Its own composable shows nothing while the mic is live and silent, so the slot is held open:
    // otherwise the column shrinks, and since it is centred vertically, the whole group jumps.
    // Abschaltbar über "Microphone level". Dann entfällt auch der Platzhalter -- ein leerer Slot
    // für etwas, das nie erscheint, verschöbe die Knöpfe ohne Gegenwert.
    if (SignalStore.accessibility.showsMicLevel) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(MIC_SLOT_SIZE)
      ) {
        if (localParticipant != null) {
          ParticipantAudioIndicator(
            participant = localParticipant,
            selfPipMode = SelfPipMode.NOT_SELF_PIP
          )
        }
      }
    }
  }
}

/**
 * How loud it is right now, and whether it is all the way down. Drawn rather than spoken in numbers:
 * "3 of 5" means nothing without knowing the scale, a filling bar does.
 */
@Composable
private fun VolumeLevel(
  volume: Int,
  minVolume: Int,
  maxVolume: Int
) {
  val span = (maxVolume - minVolume).coerceAtLeast(1)
  val filled = ((volume - minVolume).toFloat() / span).coerceIn(0f, 1f)
  val isSilent = volume <= minVolume

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = spacedBy(8.dp)
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(
        id = if (isSilent) R.drawable.ic_speaker_off_outline_24 else R.drawable.symbol_speaker_fill_white_24
      ),
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(24.dp)
    )

    Box(
      modifier = Modifier
        .width(8.dp)
        .height(96.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color.White.copy(alpha = 0.3f))
    ) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(filled)
          .background(Color.White)
      )
    }
  }
}

@Composable
private fun VolumeButton(
  imageVector: ImageVector,
  contentDescription: String,
  onClick: () -> Unit
) {
  val buttonSize = dimensionResource(id = R.dimen.webrtc_button_size)
  val contentColor: Color = colorResource(id = CoreUiR.color.signal_light_colorOnPrimary)

  IconButtons.IconButton(
    onClick = onClick,
    size = buttonSize,
    modifier = Modifier.size(buttonSize),
    colors = IconButtons.iconButtonColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = contentColor
    )
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      modifier = Modifier.size(24.dp),
      tint = contentColor
    )
  }
}
