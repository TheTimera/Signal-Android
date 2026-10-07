/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Dividers
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.lock.v2.CreateSvrPinActivity
import org.thoughtcrime.securesms.util.navigation.safeNavigate

/**
 * Ten per cent steps, and no zero: the point of the setting is that a call never starts silent, so
 * silence is not on the list.
 */
private val VOLUME_VALUES = (10..100 step 10).map { it.toString() }.toTypedArray()

// The labels are built inside the composable: "%1$d %%" can be translated, a "$it %" assembled here
// could not. Some languages place the percent sign differently, or space it differently.
@Composable
private fun volumeLabels(): Array<String> = VOLUME_VALUES.map {
  stringResource(R.string.Accessibility__percent, it.toInt())
}.toTypedArray()

/**
 * Caregiver-facing configuration for Accessibility Mode.
 *
 * The structure follows the design (screen 130); the wording was rewritten to match Signal's own
 * conventions ("Signal PIN", sentence case titles, descriptions as full sentences) and to describe
 * what the code actually does rather than what the mode is called.
 */
class AccessibilityModeSettingsFragment : ComposeFragment() {

  private val viewModel: AccessibilityModeSettingsViewModel by viewModels()

  override fun onResume() {
    super.onResume()
    // The caregiver can leave to create a PIN or change the exit method; without this the screen
    // would keep showing the state from before they left.
    viewModel.refresh()
  }

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsState()

    AccessibilityModeSettingsContent(
      state = state,
      onEvent = viewModel::onEvent,
      onNavigationClick = { findNavController().popBackStack() },
      onExitSettingsClick = {
        findNavController().safeNavigate(R.id.action_accessibilityModeSettingsFragment_to_accessibilityExitSettingsFragment)
      },
      onMoreInformationClick = {
        findNavController().safeNavigate(R.id.action_accessibilityModeSettingsFragment_to_accessibilityModeInfoFragment)
      },
      onCreatePinClick = {
        startActivity(CreateSvrPinActivity.getIntentForPinCreate(requireContext()))
      },
      onActivated = {
        // Closes settings so the caregiver lands straight on the simplified home screen and can
        // see what they just switched on, instead of having to navigate back for it.
        requireActivity().finish()
      }
    )
  }
}

@Composable
private fun AccessibilityModeSettingsContent(
  state: AccessibilityModeSettingsState,
  onEvent: (AccessibilityModeSettingsEvents) -> Unit,
  onNavigationClick: () -> Unit,
  onExitSettingsClick: () -> Unit,
  onMoreInformationClick: () -> Unit,
  onCreatePinClick: () -> Unit,
  onActivated: () -> Unit
) {
  Scaffolds.Settings(
    title = stringResource(R.string.Accessibility__accessibility_mode),
    navigationContentDescription = stringResource(R.string.Accessibility__go_back),
    navigationIcon = SignalIcons.ArrowStart.imageVector,
    onNavigationClick = onNavigationClick
  ) { contentPadding ->
    LazyColumn(
      contentPadding = contentPadding
    ) {
      item {
        Rows.TextRow(
          text = stringResource(R.string.Accessibility__how_to_exit),
          label = if (state.hasPin) {
            stringResource(R.string.Accessibility__pin_unlocks_these_settings)
          } else {
            stringResource(R.string.Accessibility__no_pin_tap_pattern_used)
          },
          onClick = onExitSettingsClick
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.iconsAlwaysVisible,
          text = stringResource(R.string.Accessibility__always_show_call_controls),
          label = stringResource(R.string.Accessibility__always_show_call_controls_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleIconsAlwaysVisible(it)) }
        )
      }

      item {
        Dividers.Default()
      }

      item {
        Rows.ToggleRow(
          checked = state.hideMessageNotifications,
          text = stringResource(R.string.Accessibility__hide_message_notifications),
          label = stringResource(R.string.Accessibility__hide_message_notifications_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleHideMessageNotifications(it)) }
        )
      }

      item {
        Text(
          text = stringResource(R.string.Accessibility__video_only_header),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .horizontalGutters()
            .padding(bottom = 8.dp)
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.allowCameraToggle,
          text = stringResource(R.string.Accessibility__camera_button),
          label = stringResource(R.string.Accessibility__camera_button_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowCameraToggle(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.allowAnswerWithoutVideo,
          text = stringResource(R.string.Accessibility__answer_without_video_button),
          label = stringResource(R.string.Accessibility__answer_without_video_button_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowAnswerWithoutVideo(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.allowMicToggle,
          text = stringResource(R.string.Accessibility__microphone_button),
          label = stringResource(R.string.Accessibility__microphone_button_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowMicToggle(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.allowCameraSwitch,
          text = stringResource(R.string.Accessibility__camera_switch_button),
          label = stringResource(R.string.Accessibility__camera_switch_button_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowCameraSwitch(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.showMicLevel,
          text = stringResource(R.string.Accessibility__microphone_level),
          label = stringResource(R.string.Accessibility__microphone_level_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleShowMicLevel(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.speakerAlwaysOn,
          text = stringResource(R.string.Accessibility__always_use_the_speaker),
          label = stringResource(R.string.Accessibility__always_use_the_speaker_label),
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleSpeakerAlwaysOn(it)) }
        )
      }

      item {
        Rows.RadioListRow(
          text = stringResource(R.string.Accessibility__volume_at_the_start_of_a_call),
          labels = volumeLabels(),
          values = VOLUME_VALUES,
          selectedValue = state.callStartVolumePercent.toString(),
          onSelected = { onEvent(AccessibilityModeSettingsEvents.SetCallStartVolumePercent(it.toInt())) }
        )
      }

      // The same condition swaps switch for notice, rather than a second one that could drift.
      item {
        if (state.screenShareAvailable) {
          Rows.ToggleRow(
            checked = state.allowScreenShare,
            text = stringResource(R.string.Accessibility__screen_sharing_button),
            label = stringResource(R.string.Accessibility__screen_sharing_button_label),
            onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowScreenShare(it)) }
          )
        } else {
          Rows.TextRow(
            text = stringResource(R.string.Accessibility__screen_sharing_button),
            label = stringResource(R.string.Accessibility__screen_sharing_unavailable_label)
          )
        }
      }

      item {
        Buttons.LargePrimary(
          onClick = {
            if (state.enabled) {
              onEvent(AccessibilityModeSettingsEvents.Deactivate)
            } else {
              // Never activates directly: the dialog is what guarantees a way back out exists.
              onEvent(AccessibilityModeSettingsEvents.RequestActivation)
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .horizontalGutters()
            .padding(top = 16.dp, bottom = 24.dp)
        ) {
          Text(text = if (state.enabled) stringResource(R.string.Accessibility__turn_off_accessibility_mode) else stringResource(R.string.Accessibility__turn_on_accessibility_mode))
        }
      }
    }

    if (state.showActivationDialog) {
      AccessibilityActivationDialog(
        hasPin = state.hasPin,
        pinIsExitMethod = state.pinIsExitMethod,
        onDismiss = { onEvent(AccessibilityModeSettingsEvents.DismissActivationDialog) },
        onMoreInformation = {
          onEvent(AccessibilityModeSettingsEvents.DismissActivationDialog)
          onMoreInformationClick()
        },
        onSetUpTapPattern = {
          onEvent(AccessibilityModeSettingsEvents.DismissActivationDialog)
          onExitSettingsClick()
        },
        onCreatePin = {
          onEvent(AccessibilityModeSettingsEvents.DismissActivationDialog)
          onCreatePinClick()
        },
        onConfirm = {
          onEvent(AccessibilityModeSettingsEvents.Activate)
          onActivated()
        }
      )
    }
  }
}
