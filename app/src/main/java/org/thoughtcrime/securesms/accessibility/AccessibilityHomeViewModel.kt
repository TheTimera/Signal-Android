/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.accessibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies

/**
 * The design shows three tiles. Four 280dp tiles plus their gaps still fit the 1280dp width of the
 * target tablet in one centred row; more would overflow. A cap, not a design statement.
 */
private const val MAX_CONTACTS = 4

class AccessibilityHomeViewModel : ViewModel() {

  private val _state = MutableStateFlow(AccessibilityHomeState())
  val state: StateFlow<AccessibilityHomeState> = _state

  init {
    load()
  }

  fun load() {
    viewModelScope.launch(Dispatchers.IO) {
      val context = AppDependencies.application
      val contacts = mutableListOf<AccessibilityContact>()

      val threadTable = SignalDatabase.threads
      threadTable.readerFor(
        threadTable.getRecentConversationList(
          MAX_CONTACTS,
          false, // includeInactiveGroups
          true, // individualsOnly -- a simplified video call surface has no use for group threads
          false, // groupsOnly
          true, // hideV1Groups
          true, // hideSms
          true // hideSelf -- calling yourself is not a useful tile
        )
      ).use { reader ->
        var record = reader.getNext()
        while (record != null && contacts.size < MAX_CONTACTS) {
          val recipient = record.recipient.resolve()
          contacts += AccessibilityContact(id = recipient.id, name = recipient.getDisplayName(context))
          record = reader.getNext()
        }
      }

      _state.value = AccessibilityHomeState(loading = false, contacts = contacts)
    }
  }
}
