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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.events.WebRtcViewModel

/**
 * The design shows three tiles. Four 280dp tiles plus their gaps still fit the 1280dp width of the
 * target tablet in one centred row; more would overflow. A cap, not a design statement.
 */
private const val MAX_CONTACTS = 4

/** How long the "call ended" notice stays up. */
const val ENDED_CALL_NOTICE_MILLIS = 10_000L

class AccessibilityHomeViewModel : ViewModel() {

  private val _state = MutableStateFlow(AccessibilityHomeState())
  val state: StateFlow<AccessibilityHomeState> = _state

  /**
   * When the call that is currently running connected, or -1 while none is. Only a call that was
   * actually connected gets a duration -- one that was never picked up has nothing to report.
   */
  private var connectedAtMillis = -1L

  init {
    load()
    EventBus.getDefault().register(this)
  }

  override fun onCleared() {
    EventBus.getDefault().unregister(this)
  }

  /**
   * Reads the tiles. Call this again whenever the screen comes back into view: names follow their
   * recipient, so a nickname set in the meantime would otherwise never arrive.
   */
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

      _state.update { it.copy(loading = false, contacts = contacts) }
    }
  }

  /** Takes the notice down once it has had its time. */
  fun dismissEndedCall() {
    _state.update { it.copy(endedCall = null) }
  }

  /**
   * Signal publishes call state as a sticky EventBus event -- the same source its own job constraints
   * read to tell whether a call is running. Subscribing here rather than reading the call log keeps
   * the duration the one the call screen itself counted.
   *
   * Sticky delivery means the last event arrives again on registration. That is harmless: without a
   * CALL_CONNECTED seen in this process [connectedAtMillis] stays -1, and no notice is raised.
   */
  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  fun onCallEvent(event: WebRtcViewModel) {
    when (event.state) {
      WebRtcViewModel.State.CALL_CONNECTED -> {
        if (connectedAtMillis == -1L) {
          connectedAtMillis = if (event.callConnectedTime > 0) event.callConnectedTime else System.currentTimeMillis()
        }
      }

      WebRtcViewModel.State.IDLE,
      WebRtcViewModel.State.CALL_DISCONNECTED,
      WebRtcViewModel.State.CALL_DISCONNECTED_GLARE,
      WebRtcViewModel.State.CALL_ACCEPTED_ELSEWHERE,
      WebRtcViewModel.State.CALL_DECLINED_ELSEWHERE -> {
        if (connectedAtMillis != -1L) {
          val now = System.currentTimeMillis()
          _state.update {
            it.copy(
              endedCall = AccessibilityEndedCall(
                durationMillis = now - connectedAtMillis,
                endedAtMillis = now
              )
            )
          }
          connectedAtMillis = -1L

          // The other side may have become a contact, or their name may have changed while talking.
          load()
        }
      }

      else -> Unit
    }
  }
}
