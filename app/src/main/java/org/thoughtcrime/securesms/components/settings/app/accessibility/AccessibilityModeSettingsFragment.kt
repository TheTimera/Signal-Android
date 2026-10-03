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
    title = "Accessibility Mode",
    navigationContentDescription = "Go back",
    navigationIcon = SignalIcons.ArrowStart.imageVector,
    onNavigationClick = onNavigationClick
  ) { contentPadding ->
    LazyColumn(
      contentPadding = contentPadding
    ) {
      item {
        Rows.TextRow(
          text = "How to exit Accessibility Mode",
          label = if (state.hasPin) {
            "Your Signal PIN unlocks these settings. You can switch to a tap pattern instead."
          } else {
            "You have no Signal PIN, so a tap pattern will be used to unlock these settings."
          },
          onClick = onExitSettingsClick
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.iconsAlwaysVisible,
          text = "Always show call controls",
          label = "Hang up, volume, and any other buttons you allow stay on screen for the whole call. Normally they fade out and return when the screen is tapped.",
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleIconsAlwaysVisible(it)) }
        )
      }

      item {
        Dividers.Default()
      }

      item {
        Text(
          text = "Calls are video only by default. You can add buttons that let the person turn off their camera or microphone, or switch between the front and rear camera.",
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
          text = "Allow turning the camera off",
          label = "Adds a camera button to the call screen.",
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowCameraToggle(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.allowMicToggle,
          text = "Allow muting the microphone",
          label = "Adds a microphone button to the call screen.",
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowMicToggle(it)) }
        )
      }

      item {
        Rows.ToggleRow(
          checked = state.allowCameraSwitch,
          text = "Allow switching cameras",
          label = "Adds a button to switch between the front and rear camera.",
          onCheckChanged = { onEvent(AccessibilityModeSettingsEvents.ToggleAllowCameraSwitch(it)) }
        )
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
          Text(text = if (state.enabled) "Turn off Accessibility Mode" else "Turn on Accessibility Mode")
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
