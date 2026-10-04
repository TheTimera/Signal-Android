/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Dividers
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters
import org.thoughtcrime.securesms.keyvalue.AccessibilityTapCorner

/**
 * Lets the caregiver choose how they get back out of Accessibility Mode: the Signal PIN, or a tap
 * pattern on the contact overview screen.
 */
class AccessibilityExitSettingsFragment : ComposeFragment() {

  private val viewModel: AccessibilityExitSettingsViewModel by viewModels()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsState()

    AccessibilityExitSettingsContent(
      state = state,
      onEvent = viewModel::onEvent,
      onNavigationClick = { findNavController().popBackStack() }
    )
  }
}

private val CORNER_LABELS = arrayOf("Top left corner", "Top right corner", "Bottom left corner", "Bottom right corner")
private val CORNER_VALUES = arrayOf(
  AccessibilityTapCorner.TOP_LEFT.name,
  AccessibilityTapCorner.TOP_RIGHT.name,
  AccessibilityTapCorner.BOTTOM_LEFT.name,
  AccessibilityTapCorner.BOTTOM_RIGHT.name
)

private val TAP_COUNT_VALUES = arrayOf("3", "4", "5", "6", "7", "8", "9", "10")

private val TAP_WINDOW_LABELS = arrayOf("1 second", "2 seconds", "3 seconds", "5 seconds")
private val TAP_WINDOW_VALUES = arrayOf("1000", "2000", "3000", "5000")

@Composable
private fun AccessibilityExitSettingsContent(
  state: AccessibilityExitSettingsState,
  onEvent: (AccessibilityExitSettingsEvents) -> Unit,
  onNavigationClick: () -> Unit
) {
  Scaffolds.Settings(
    title = "How to exit Accessibility mode",
    navigationContentDescription = "Go back",
    navigationIcon = SignalIcons.ArrowStart.imageVector,
    onNavigationClick = onNavigationClick
  ) { contentPadding ->
    LazyColumn(
      contentPadding = contentPadding
    ) {
      item {
        Rows.ToggleRow(
          checked = state.exitWithPin && state.hasPin,
          text = "Signal PIN",
          label = if (state.hasPin) {
            "Your Signal PIN gets you back out of Accessibility mode. With this off, the tap pattern below is the only way out."
          } else {
            "You have no Signal PIN, so the tap pattern below is the only way out. Set up a PIN in Settings to use this."
          },
          enabled = state.hasPin,
          onCheckChanged = { onEvent(AccessibilityExitSettingsEvents.ToggleExitWithPin(it)) }
        )
      }

      item {
        Dividers.Default()
      }

      item {
        Text(
          text = "Tap pattern",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .horizontalGutters()
            .padding(bottom = 4.dp)
        )
      }

      item {
        Rows.RadioListRow(
          text = "Corner to tap",
          labels = CORNER_LABELS,
          values = CORNER_VALUES,
          selectedValue = state.tapCorner.name,
          enabled = state.tapMethodActive,
          onSelected = { onEvent(AccessibilityExitSettingsEvents.SetTapCorner(AccessibilityTapCorner.valueOf(it))) }
        )
      }

      item {
        Rows.RadioListRow(
          text = "Number of taps",
          labels = TAP_COUNT_VALUES,
          values = TAP_COUNT_VALUES,
          selectedValue = state.tapCount.toString(),
          enabled = state.tapMethodActive,
          onSelected = { onEvent(AccessibilityExitSettingsEvents.SetTapCount(it.toInt())) }
        )
      }

      item {
        Rows.RadioListRow(
          text = "Time limit",
          labels = TAP_WINDOW_LABELS,
          values = TAP_WINDOW_VALUES,
          selectedValue = state.tapWindowMillis.toString(),
          enabled = state.tapMethodActive,
          onSelected = { onEvent(AccessibilityExitSettingsEvents.SetTapWindowMillis(it.toInt())) }
        )
      }

      item {
        TapTargetHint(
          corner = state.tapCorner,
          tapCount = state.tapCount,
          tapWindowMillis = state.tapWindowMillis,
          modifier = Modifier
            .horizontalGutters()
            .padding(top = 16.dp, bottom = 24.dp)
        )
      }
    }
  }
}

/**
 * The picture from the design: an outline of the screen with a dot in the chosen corner, next to a
 * sentence spelling the gesture out.
 */
@Composable
private fun TapTargetHint(
  corner: AccessibilityTapCorner,
  tapCount: Int,
  tapWindowMillis: Int,
  modifier: Modifier = Modifier
) {
  val alignment = when (corner) {
    AccessibilityTapCorner.TOP_LEFT -> Alignment.TopStart
    AccessibilityTapCorner.TOP_RIGHT -> Alignment.TopEnd
    AccessibilityTapCorner.BOTTOM_LEFT -> Alignment.BottomStart
    AccessibilityTapCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
  }

  val seconds = tapWindowMillis / 1000f
  val secondsText = if (seconds == seconds.toInt().toFloat()) "${seconds.toInt()}" else "$seconds"

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .height(110.dp)
        .fillMaxWidth(0.45f)
        .border(
          width = 2.dp,
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(12.dp)
        )
        .padding(10.dp)
    ) {
      Box(
        modifier = Modifier
          .align(alignment)
          .size(18.dp)
          .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
      )
    }

    Text(
      text = "Tap the contact overview screen $tapCount times in $secondsText seconds to leave Accessibility mode.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 16.dp)
    )
  }
}
