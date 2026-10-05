/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.webrtc.v2

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.signal.core.ui.compose.NightPreview
import org.signal.core.ui.compose.SignalPreviewWrapper
import org.signal.core.ui.compose.TriggerAlignedPopupState.Companion.popupTrigger
import org.signal.core.ui.compose.TriggerAlignedPopupState.Companion.rememberTriggerAlignedPopupState
import org.signal.core.ui.compose.spaceBetweenUpTo
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.webrtc.CallParticipantsState
import org.thoughtcrime.securesms.components.webrtc.ToggleButtonOutputState
import org.thoughtcrime.securesms.components.webrtc.WebRtcAudioOutput
import org.thoughtcrime.securesms.components.webrtc.WebRtcControls
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.RemoteConfig

/**
 * Renders the button strip / start call button in the call screen
 * bottom sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallControls(
  displayVideoTooltip: Boolean,
  callControlsState: CallControlsState,
  callScreenControlsListener: CallScreenControlsListener,
  callScreenSheetDisplayListener: CallScreenSheetDisplayListener,
  additionalActionsState: AdditionalActionsState,
  audioOutputPickerController: AudioOutputPickerController,
  modifier: Modifier = Modifier
) {
  val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
  val simplified = SignalStore.accessibility.simplifiesCallScreen

  // Nur im Modus trägt "Start Video Call" ein Symbol, und zwar dasselbe, das AcceptCallButton für
  // denselben Fall nimmt. null heißt: Signals Knopf bleibt wie er ist.
  val startCallIcon = if (simplified) {
    ImageVector.vectorResource(
      id = if (callControlsState.isVideoEnabled) R.drawable.symbol_video_fill_24 else R.drawable.symbol_phone_fill_white_24
    )
  } else {
    null
  }

  val density = LocalDensity.current
  val bottom = with(density) { WindowInsets.navigationBarsIgnoringVisibility.getBottom(density).toDp() }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = spacedBy(30.dp),
    modifier = modifier.padding(bottom = bottom)
  ) {
    Row(
      horizontalArrangement = Arrangement.spaceBetweenUpTo(20.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
      // In the mode the back arrow lives in this strip rather than in the top bar, and it is the
      // first item in the row -- which is where a back arrow is looked for. Before, it sat between
      // the hangup and "Start Video Call", so it read as part of that decision rather than as the
      // way out of it.
      //
      // What this does NOT buy, and is not meant to: a fixed spot on screen. The row centres its
      // group -- spaceBetweenUpTo caps the gaps and the remaining width becomes outer margin --
      // so the arrow is leftmost WITHIN THE GROUP and its absolute position still shifts when
      // toggles are switched on or off (measured x=389 of 1280 with all three on). Pinning it to
      // the sheet edge was built and rejected on 4.10.2026: leftmost in the group is the wanted
      // behaviour. Hence no Box wrapper -- if one shows up here again, this is why it was removed.
      if (simplified && callControlsState.displayStartCallButton) {
        CallBackButton(onClick = callScreenControlsListener::onNavigateUpClicked)
      }

      // Never in the mode. Signal shows this button only while the local video is off or a headset
      // is attached (WebRtcControls.displayAudioToggle), which in a video-only mode means only for
      // the moment between answering and the camera starting -- it flashed and vanished. The route
      // is decided by "Always use the speaker" instead.
      if (callControlsState.displayAudioOutputToggle && !simplified) {
        CallAudioToggleButton(
          contentDescription = stringResource(id = R.string.WebRtcAudioOutputToggle__audio_output),
          onSheetDisplayChanged = callScreenSheetDisplayListener::onAudioDeviceSheetDisplayChanged,
          pickerController = audioOutputPickerController,
          enabled = !callControlsState.isAudioOutputChangePending
        )
      }

      val hasCameraPermission = ContextCompat.checkSelfPermission(LocalContext.current, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
      if (callControlsState.displayVideoToggle && !callControlsState.isLocalScreenSharing) {
        CallScreenTooltipBox(
          text = stringResource(R.string.WebRtcCallActivity__tap_here_to_turn_on_your_video),
          displayTooltip = displayVideoTooltip,
          onTooltipDismissed = callScreenSheetDisplayListener::onVideoTooltipDismissed
        ) {
          ToggleVideoButton(
            isVideoEnabled = callControlsState.isVideoEnabled && hasCameraPermission,
            onChange = callScreenControlsListener::onVideoChanged
          )
        }
      }

      val hasRecordAudioPermission = ContextCompat.checkSelfPermission(LocalContext.current, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
      if (callControlsState.displayMicToggle) {
        ToggleMicButton(
          isMicEnabled = callControlsState.isMicEnabled && hasRecordAudioPermission,
          onChange = callScreenControlsListener::onMicChanged
        )
      }

      if (callControlsState.displayGroupRingingToggle) {
        ToggleRingButton(
          isRingEnabled = callControlsState.isGroupRingingEnabled,
          isRingAllowed = callControlsState.isGroupRingingAllowed,
          onChange = callScreenControlsListener::onRingGroupChanged
        )
      }

      // Accessibility Mode puts the camera switch where the overflow used to be: raising a hand and
      // sharing a screen are not things its user does, switching cameras is.
      // Three conditions, all of them load-bearing: the caregiver allowed it, the device really has
      // a second camera (Camera.flip() asserts and kills the process otherwise), and video is on.
      if (simplified &&
        SignalStore.accessibility.allowCameraSwitch &&
        callControlsState.isMoreThanOneCameraAvailable &&
        callControlsState.isVideoEnabled
      ) {
        SwitchCameraDirectionButton(onClick = callScreenControlsListener::onCameraDirectionChanged)
      }

      if (simplified && SignalStore.accessibility.allowScreenShare && callControlsState.displayScreenShareToggle) {
        ToggleScreenShareButton(
          isScreenSharing = callControlsState.isLocalScreenSharing,
          onChange = callScreenControlsListener::onScreenShareChanged
        )
      }

      if (!simplified && callControlsState.displayAdditionalActions) {
        AdditionalActionsButton(
          onClick = callScreenControlsListener::onOverflowClicked,
          modifier = Modifier.popupTrigger(additionalActionsState.triggerAlignedPopupState)
        )
      }

      if (callControlsState.displayEndCallButton) {
        if (simplified) {
          EndVideoCallButton(onClick = callScreenControlsListener::onEndCallPressed)
        } else {
          HangupButton(onClick = callScreenControlsListener::onEndCallPressed)
        }
      }

      if (callControlsState.displayStartCallButton && !isPortrait) {
        StartCallButton(
          text = stringResource(callControlsState.startCallButtonText),
          onClick = {
            callScreenControlsListener.onStartCall(callControlsState.isVideoEnabled)
          },
          imageVector = startCallIcon
        )
      }
    }

    if (callControlsState.displayStartCallButton && isPortrait) {
      StartCallButton(
        text = stringResource(callControlsState.startCallButtonText),
        onClick = {
          callScreenControlsListener.onStartCall(callControlsState.isVideoEnabled)
        },
        imageVector = startCallIcon
      )
    }
  }
}

/**
 * The distinct control layouts the call screen can produce, as derived by
 * [CallControlsState.fromViewModelData] from a given [WebRtcControls]. The rules that separate them:
 *
 * - the audio, video, and mic toggles appear in every pre-join and in-call state, except that the audio toggle
 *   drops out once your own camera is on and there's no headset to switch to
 * - the ring toggle is pre-join only, and only for a group that isn't a call link and that nobody has joined yet
 * - the overflow button is in-call only, and only once someone else is on the call
 * - the start call button is pre-join only, and the hangup button replaces it once the call is up
 *
 * Note that a connected call link and a connected group produce the same strip -- a call link is a group call with
 * `isCallLink` set, and that flag only suppresses the ring toggle, which is pre-join only. So the connected case is
 * covered once, by [GROUP_ONGOING].
 */
private enum class CallControlsPreviewState(val controls: CallControlsState) {
  /** 1:1 lobby. No ring toggle (not a group), no hangup yet. */
  ONE_TO_ONE_PRE_JOIN(
    CallControlsState(
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.HANDSET,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      displayStartCallButton = true,
      startCallButtonText = R.string.WebRtcCallView__start_call
    )
  ),

  /** The smallest strip: camera on with no headset attached leaves nowhere for the audio toggle to route to. */
  ONE_TO_ONE_PRE_JOIN_VIDEO(
    CallControlsState(
      audioOutput = WebRtcAudioOutput.SPEAKER,
      displayVideoToggle = true,
      isVideoEnabled = true,
      displayMicToggle = true,
      isMicEnabled = true,
      displayStartCallButton = true,
      startCallButtonText = R.string.WebRtcCallView__start_call
    )
  ),

  /** Connected 1:1. Overflow only shows here when screen sharing is enabled remotely. */
  ONE_TO_ONE_ONGOING(
    CallControlsState(
      isBluetoothHeadsetAvailable = true,
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.BLUETOOTH_HEADSET,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      displayAdditionalActions = true,
      displayEndCallButton = true
    )
  ),

  /** Group lobby with nobody on the call yet: the only state that offers the ring toggle. */
  GROUP_PRE_JOIN(
    CallControlsState(
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.HANDSET,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      displayGroupRingingToggle = true,
      isGroupRingingEnabled = true,
      isGroupRingingAllowed = true,
      isGroupCall = true,
      displayStartCallButton = true,
      startCallButtonText = R.string.WebRtcCallView__start_call
    )
  ),

  /** Same lobby, but the group is past [RemoteConfig.maxGroupCallRingSize], so the ring toggle is shown disabled. */
  GROUP_PRE_JOIN_RING_DISALLOWED(
    CallControlsState(
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.HANDSET,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      displayGroupRingingToggle = true,
      isGroupCall = true,
      displayStartCallButton = true,
      startCallButtonText = R.string.WebRtcCallView__start_call
    )
  ),

  /** Group lobby for a call that's already running: the ring toggle is gone and the button reads "Join call". */
  GROUP_PRE_JOIN_CALL_IN_PROGRESS(
    CallControlsState(
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.HANDSET,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      isGroupCall = true,
      displayStartCallButton = true,
      startCallButtonText = R.string.WebRtcCallView__join_call
    )
  ),

  /** Call link lobby. Identical to an empty group lobby except that call links never ring. */
  CALL_LINK_PRE_JOIN(
    CallControlsState(
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.HANDSET,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      isGroupCall = true,
      displayStartCallButton = true,
      startCallButtonText = R.string.WebRtcCallView__start_call
    )
  ),

  /** Connected group or call link. The widest strip. */
  GROUP_ONGOING(
    CallControlsState(
      isWiredHeadsetAvailable = true,
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.WIRED_HEADSET,
      displayVideoToggle = true,
      isVideoEnabled = true,
      displayMicToggle = true,
      isMicEnabled = true,
      isGroupCall = true,
      displayAdditionalActions = true,
      displayEndCallButton = true
    )
  ),

  /** Connected group while sharing your screen, which takes the video toggle away. */
  GROUP_ONGOING_SCREEN_SHARING(
    CallControlsState(
      isEarpieceAvailable = true,
      displayAudioOutputToggle = true,
      audioOutput = WebRtcAudioOutput.SPEAKER,
      displayVideoToggle = true,
      displayMicToggle = true,
      isMicEnabled = true,
      isGroupCall = true,
      displayAdditionalActions = true,
      displayEndCallButton = true,
      isLocalScreenSharing = true
    )
  )
}

/**
 * The remembered collaborators [CallControls] needs beyond its state, built to match the given controls so that
 * the audio picker offers the same devices the strip claims are available.
 */
@Composable
private fun rememberPreviewState(controls: CallControlsState): Pair<AdditionalActionsState, AudioOutputPickerController> {
  val triggerAlignedPopupState = rememberTriggerAlignedPopupState()
  val outputState = remember(controls) {
    ToggleButtonOutputState().apply {
      isEarpieceAvailable = controls.isEarpieceAvailable
      isWiredHeadsetAvailable = controls.isWiredHeadsetAvailable
      isBluetoothHeadsetAvailable = controls.isBluetoothHeadsetAvailable
      setCurrentOutput(controls.audioOutput)
    }
  }

  return remember(controls, triggerAlignedPopupState, outputState) {
    AdditionalActionsState(
      triggerAlignedPopupState = triggerAlignedPopupState,
      isGroupCall = controls.isGroupCall,
      isScreenSharing = controls.isLocalScreenSharing
    ) to AudioOutputPickerController(
      outputState = outputState,
      onSelectedDeviceChanged = {}
    )
  }
}

@Composable
private fun CallControlsPreview(previewState: CallControlsPreviewState) {
  val (additionalActionsState, audioOutputPickerController) = rememberPreviewState(previewState.controls)

  CallControls(
    callControlsState = previewState.controls,
    displayVideoTooltip = false,
    callScreenControlsListener = CallScreenControlsListener.Empty,
    callScreenSheetDisplayListener = CallScreenSheetDisplayListener.Empty,
    additionalActionsState = additionalActionsState,
    audioOutputPickerController = audioOutputPickerController
  )
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun OneToOnePreJoinCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.ONE_TO_ONE_PRE_JOIN)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun OneToOnePreJoinVideoCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.ONE_TO_ONE_PRE_JOIN_VIDEO)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun OneToOneOngoingCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.ONE_TO_ONE_ONGOING)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun GroupPreJoinCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.GROUP_PRE_JOIN)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun GroupPreJoinRingDisallowedCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.GROUP_PRE_JOIN_RING_DISALLOWED)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun GroupPreJoinCallInProgressCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.GROUP_PRE_JOIN_CALL_IN_PROGRESS)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun CallLinkPreJoinCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.CALL_LINK_PRE_JOIN)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun GroupOngoingCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.GROUP_ONGOING)
}

@PreviewWrapper(SignalPreviewWrapper::class)
@NightPreview
@Composable
private fun GroupOngoingScreenSharingCallControlsPreview() {
  CallControlsPreview(CallControlsPreviewState.GROUP_ONGOING_SCREEN_SHARING)
}

/**
 * Callbacks for call controls actions.
 */
interface CallScreenSheetDisplayListener {
  fun onAudioDeviceSheetDisplayChanged(displayed: Boolean)
  fun onOverflowDisplayChanged(displayed: Boolean)
  fun onVideoTooltipDismissed()

  object Empty : CallScreenSheetDisplayListener {
    override fun onAudioDeviceSheetDisplayChanged(displayed: Boolean) = Unit
    override fun onOverflowDisplayChanged(displayed: Boolean) = Unit
    override fun onVideoTooltipDismissed() = Unit
  }
}

/**
 * State object representing how the controls should appear. Since these values are
 * gleaned from multiple data sources, this object represents the amalgamation of those
 * sources so we don't need to listen to multiple here.
 */
data class CallControlsState(
  val isEarpieceAvailable: Boolean = false,
  val isBluetoothHeadsetAvailable: Boolean = false,
  val isWiredHeadsetAvailable: Boolean = false,
  val skipHiddenState: Boolean = true,
  val displayAudioOutputToggle: Boolean = false,
  val audioOutput: WebRtcAudioOutput = WebRtcAudioOutput.HANDSET,
  val isAudioOutputChangePending: Boolean = false,
  val displayVideoToggle: Boolean = false,
  val isVideoEnabled: Boolean = false,
  val displayMicToggle: Boolean = false,
  val isMicEnabled: Boolean = false,
  val displayGroupRingingToggle: Boolean = false,
  val isGroupRingingEnabled: Boolean = false,
  val isGroupRingingAllowed: Boolean = false,
  val isGroupCall: Boolean = false,
  val displayAdditionalActions: Boolean = false,
  val displayStartCallButton: Boolean = false,
  val startCallButtonText: Int = R.string.WebRtcCallView__start_call,
  val displayEndCallButton: Boolean = false,
  val isLocalScreenSharing: Boolean = false,
  /**
   * Signal offers screen sharing only during a call and only while its own remote flag is on; the
   * mode's button follows the same rule instead of inventing a second one.
   */
  val displayScreenShareToggle: Boolean = false,
  /**
   * Without a second camera, Camera.flip() throws an AssertionError and takes the process with it.
   * Signal's own switch button in the self preview is guarded by this; the one Accessibility Mode
   * puts in the control strip has to be too.
   */
  val isMoreThanOneCameraAvailable: Boolean = false
) {

  val hasAnyControls: Boolean
    get() = displayAudioOutputToggle ||
      displayVideoToggle ||
      displayMicToggle ||
      displayGroupRingingToggle ||
      displayAdditionalActions ||
      displayStartCallButton ||
      displayEndCallButton

  companion object {
    /**
     * Presentation-level method to build out the controls state from legacy objects.
     */
    @JvmStatic
    fun fromViewModelData(
      callParticipantsState: CallParticipantsState,
      webRtcControls: WebRtcControls,
      groupMemberCount: Int,
      isAudioDeviceChangePending: Boolean = false,
      isLocalScreenSharing: Boolean = false
    ): CallControlsState {
      return CallControlsState(
        isEarpieceAvailable = webRtcControls.isEarpieceAvailableForAudioToggle,
        isBluetoothHeadsetAvailable = webRtcControls.isBluetoothHeadsetAvailableForAudioToggle,
        isWiredHeadsetAvailable = webRtcControls.isWiredHeadsetAvailableForAudioToggle,
        // Accessibility Mode can pin the controls open, but never in picture-in-picture: there the
        // window is too small to show them and hiding them is the whole point.
        skipHiddenState = (SignalStore.accessibility.forcesControlsVisible && webRtcControls != WebRtcControls.PIP) ||
          !(webRtcControls.isFadeOutEnabled || webRtcControls == WebRtcControls.PIP || webRtcControls.displayErrorControls()),
        displayAudioOutputToggle = webRtcControls.displayAudioToggle(),
        audioOutput = webRtcControls.audioOutput,
        isAudioOutputChangePending = isAudioDeviceChangePending,
        displayVideoToggle = webRtcControls.displayVideoToggle() && SignalStore.accessibility.mayToggleCamera,
        isVideoEnabled = callParticipantsState.localParticipant.isVideoEnabled,
        displayMicToggle = webRtcControls.displayMuteAudio() && SignalStore.accessibility.mayToggleMic,
        isMicEnabled = callParticipantsState.localParticipant.isMicrophoneEnabled,
        displayGroupRingingToggle = webRtcControls.displayRingToggle(),
        isGroupCall = webRtcControls.isGroupCall,
        isGroupRingingEnabled = callParticipantsState.ringGroup,
        isGroupRingingAllowed = groupMemberCount <= RemoteConfig.maxGroupCallRingSize,
        displayAdditionalActions = webRtcControls.displayOverflow(),
        displayStartCallButton = webRtcControls.displayStartCallControls(),
        // "Start Call" is ambiguous where the mode only ever places video calls.
        startCallButtonText = if (SignalStore.accessibility.simplifiesCallScreen) {
          R.string.Accessibility__start_video_call
        } else {
          webRtcControls.startCallButtonText
        },
        displayEndCallButton = webRtcControls.displayEndCall(),
        isLocalScreenSharing = isLocalScreenSharing,
        // Same pair of conditions CallScreen.kt uses for the overflow entry.
        displayScreenShareToggle = webRtcControls.displayEndCall() && RemoteConfig.screenSharing,
        isMoreThanOneCameraAvailable = callParticipantsState.localParticipant.isMoreThanOneCameraAvailable
      )
    }
  }
}
