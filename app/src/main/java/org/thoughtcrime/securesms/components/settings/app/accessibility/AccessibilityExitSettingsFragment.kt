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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Dividers
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.keyvalue.AccessibilityTapCorner
import org.thoughtcrime.securesms.keyvalue.AccessibilityValues

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

// ⚠️ Beschriftungen erst im Composable aufloesen: auf oberster Ebene gibt es keinen Context und
// damit keine Uebersetzung. Die Reihenfolge muss zu CORNER_VALUES passen.
@Composable
private fun cornerLabels(): Array<String> = arrayOf(
  stringResource(R.string.Accessibility__top_left_corner),
  stringResource(R.string.Accessibility__top_right_corner),
  stringResource(R.string.Accessibility__bottom_left_corner),
  stringResource(R.string.Accessibility__bottom_right_corner)
)
private val CORNER_VALUES = arrayOf(
  AccessibilityTapCorner.TOP_LEFT.name,
  AccessibilityTapCorner.TOP_RIGHT.name,
  AccessibilityTapCorner.BOTTOM_LEFT.name,
  AccessibilityTapCorner.BOTTOM_RIGHT.name
)

private val TAP_COUNT_VALUES = arrayOf("3", "4", "5", "6", "7", "8", "9", "10")

// "1 second" hat eine eigene Zeichenkette, weil der Singular nicht in jeder Sprache durch
// Einsetzen einer 1 entsteht. Reihenfolge wie TAP_WINDOW_VALUES.
@Composable
private fun tapWindowLabels(): Array<String> = arrayOf(
  stringResource(R.string.Accessibility__one_second),
  stringResource(R.string.Accessibility__seconds, "2"),
  stringResource(R.string.Accessibility__seconds, "3"),
  stringResource(R.string.Accessibility__seconds, "5")
)
private val TAP_WINDOW_VALUES = arrayOf("1000", "2000", "3000", "5000")

/**
 * Both lists are filtered against the other one's current value, so no reachable-looking pair of
 * choices can add up to a gesture nobody can perform -- see [AccessibilityValues.MAX_TAPS_PER_SECOND].
 * Filtering rather than warning was the deliberate choice: with the Signal PIN switched off the
 * pattern is the only way back into these settings, and a warning that can be clicked past is not
 * a safeguard when the consequence is resetting Signal.
 */
private fun tapCountChoices(windowMillis: Int): Array<String> {
  val most = AccessibilityValues.maxTapCountFor(windowMillis)
  return TAP_COUNT_VALUES.filter { it.toInt() <= most }.toTypedArray()
}

@Composable
private fun tapWindowChoices(tapCount: Int): Pair<Array<String>, Array<String>> {
  val keep = TAP_WINDOW_VALUES.indices.filter {
    AccessibilityValues.isTapPatternReachable(tapCount, TAP_WINDOW_VALUES[it].toInt())
  }
  val labels = tapWindowLabels()
  return keep.map { labels[it] }.toTypedArray() to keep.map { TAP_WINDOW_VALUES[it] }.toTypedArray()
}

@Composable
private fun AccessibilityExitSettingsContent(
  state: AccessibilityExitSettingsState,
  onEvent: (AccessibilityExitSettingsEvents) -> Unit,
  onNavigationClick: () -> Unit
) {
  Scaffolds.Settings(
    title = stringResource(R.string.Accessibility__how_to_exit),
    navigationContentDescription = stringResource(R.string.Accessibility__go_back),
    navigationIcon = SignalIcons.ArrowStart.imageVector,
    onNavigationClick = onNavigationClick
  ) { contentPadding ->
    LazyColumn(
      contentPadding = contentPadding
    ) {
      item {
        Rows.ToggleRow(
          checked = state.exitWithPin && state.hasPin,
          text = stringResource(R.string.Accessibility__signal_pin),
          label = if (state.hasPin) {
            stringResource(R.string.Accessibility__pin_gets_you_out)
          } else {
            stringResource(R.string.Accessibility__no_pin_tap_pattern_only_way)
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
          text = stringResource(R.string.Accessibility__tap_pattern),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .horizontalGutters()
            .padding(bottom = 4.dp)
        )
      }

      item {
        Text(
          text = stringResource(R.string.Accessibility__tap_pattern_warning),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .horizontalGutters()
            .padding(bottom = 12.dp)
        )
      }

      item {
        Rows.RadioListRow(
          text = stringResource(R.string.Accessibility__corner_to_tap),
          labels = cornerLabels(),
          values = CORNER_VALUES,
          selectedValue = state.tapCorner.name,
          enabled = state.tapMethodActive,
          onSelected = { onEvent(AccessibilityExitSettingsEvents.SetTapCorner(AccessibilityTapCorner.valueOf(it))) }
        )
      }

      item {
        val counts = tapCountChoices(state.tapWindowMillis)
        Rows.RadioListRow(
          text = stringResource(R.string.Accessibility__number_of_taps),
          labels = counts,
          values = counts,
          selectedValue = state.tapCount.toString(),
          enabled = state.tapMethodActive,
          onSelected = { onEvent(AccessibilityExitSettingsEvents.SetTapCount(it.toInt())) }
        )
      }

      item {
        val (windowLabels, windowValues) = tapWindowChoices(state.tapCount)
        Rows.RadioListRow(
          text = stringResource(R.string.Accessibility__time_limit),
          labels = windowLabels,
          values = windowValues,
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
      text = stringResource(R.string.Accessibility__tap_hint, tapCount, secondsText),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 16.dp)
    )
  }
}
