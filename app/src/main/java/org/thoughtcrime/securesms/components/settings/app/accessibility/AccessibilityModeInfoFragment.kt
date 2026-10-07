/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters
import org.thoughtcrime.securesms.R

/**
 * The explanatory screen behind the "Need more information?" link (design screen 143). Content is
 * the designer's own text with the grammar tidied up.
 */
class AccessibilityModeInfoFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    AccessibilityModeInfoContent(
      onNavigationClick = { findNavController().popBackStack() }
    )
  }
}

@Composable
private fun AccessibilityModeInfoContent(
  onNavigationClick: () -> Unit
) {
  val paragraphs = listOf(
    stringResource(R.string.Accessibility__info_what_it_is),
    stringResource(R.string.Accessibility__info_who_sets_it_up),
    stringResource(R.string.Accessibility__info_why_locked),
    stringResource(R.string.Accessibility__info_pin_risk),
    stringResource(R.string.Accessibility__info_tap_pattern)
  )
  val tips = listOf(
    stringResource(R.string.Accessibility__info_tip_kiosk),
    stringResource(R.string.Accessibility__info_tip_tablet)
  )

  Scaffolds.Settings(
    title = stringResource(R.string.Accessibility__about_accessibility_mode),
    navigationContentDescription = stringResource(R.string.Accessibility__go_back),
    navigationIcon = SignalIcons.ArrowStart.imageVector,
    onNavigationClick = onNavigationClick
  ) { contentPadding ->
    Column(
      modifier = Modifier
        .padding(contentPadding)
        .verticalScroll(rememberScrollState())
        .horizontalGutters()
    ) {
      paragraphs.forEach { paragraph ->
        Text(
          text = paragraph,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      Text(
        text = stringResource(R.string.Accessibility__info_tip_time_limit),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 24.dp)
      )

      Text(
        text = stringResource(R.string.Accessibility__tips),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      tips.forEach { tip ->
        Text(
          text = tip,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }
    }
  }
}
