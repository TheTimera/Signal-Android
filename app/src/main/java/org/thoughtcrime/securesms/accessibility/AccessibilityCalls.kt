/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.accessibility

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.util.CommunicationActions

/**
 * Starts a video call the same way the rest of the app does, so permission prompts and the
 * already-in-a-call case behave identically. Kept here rather than in MainActivity to keep the
 * footprint inside Signal's own files as small as possible.
 */
fun startAccessibilityVideoCall(activity: FragmentActivity, recipientId: RecipientId) {
  CommunicationActions.startVideoCall(activity, Recipient.resolved(recipientId)) {
    // A plain toast rather than the usual snackbar: this screen has no scaffold to host one, and
    // silence would leave the person tapping a button that appears to do nothing.
    Toast.makeText(activity, activity.getString(R.string.Accessibility__you_are_already_in_a_call), Toast.LENGTH_SHORT).show()
  }
}
