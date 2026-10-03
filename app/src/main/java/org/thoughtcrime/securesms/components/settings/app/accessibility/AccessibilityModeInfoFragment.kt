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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters

/**
 * The explanatory screen behind "Need more information?" (design screen 143). Content is the
 * designer's own text with the grammar tidied up.
 */
class AccessibilityModeInfoFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    AccessibilityModeInfoContent(
      onNavigationClick = { findNavController().popBackStack() }
    )
  }
}

private val PARAGRAPHS = listOf(
  "Accessibility Mode is designed for people with disabilities or special needs, particularly elderly users who may feel overwhelmed by Signal's many features. It simplifies the app by offering only essential functions, starting with video calling, and lets you add more functionality as the person grows comfortable with it.",
  "Setting it up should be done by a caregiver, such as a family member, who also sets up the account on the device.",
  "To prevent unwanted changes to settings or deleted contacts, the settings are locked. Whoever sets up the device chooses the lock: the Signal PIN or a tap pattern.",
  "Choosing the Signal PIN means that if the PIN is forgotten, the account is locked for good. A Signal PIN cannot be recovered — the account would have to be deleted and set up again. The PIN is the more secure of the two.",
  "The tap pattern is the alternative. By default, whoever wants to reach the settings taps the top right corner of the screen seven times within two seconds. The corner, the number of taps and the time limit can all be changed. This method makes being locked out far less likely."
)

private val TIPS = listOf(
  "Consider running Signal in Accessibility Mode inside a kiosk app such as FreeKiosk, so no other apps distract the person using the device.",
  "Accessibility Mode suits a tablet better than a phone, simply because of the button and screen size during a video call."
)

@Composable
private fun AccessibilityModeInfoContent(
  onNavigationClick: () -> Unit
) {
  Scaffolds.Settings(
    title = "What is Accessibility Mode",
    navigationContentDescription = "Go back",
    navigationIcon = SignalIcons.ArrowStart.imageVector,
    onNavigationClick = onNavigationClick
  ) { contentPadding ->
    Column(
      modifier = Modifier
        .padding(contentPadding)
        .verticalScroll(rememberScrollState())
        .horizontalGutters()
    ) {
      PARAGRAPHS.forEach { paragraph ->
        Text(
          text = paragraph,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      Text(
        text = "Important: do not set the time limit too short, or it may not be possible to tap quickly enough.",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 24.dp)
      )

      Text(
        text = "Tips",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      TIPS.forEach { tip ->
        Text(
          text = tip,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }
    }
  }
}
