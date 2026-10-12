# Accessibility Mode — implementation notes

Companion document for the `accessibility-mode` branch. It carries the reasoning that would
otherwise sit as inline comments in the code. Signal's
[Code Style Guidelines](https://github.com/signalapp/Signal-Android/wiki/Code-Style-Guidelines) ask
for a class-level doc comment and nothing else, with an exception for "exceptionally clever code
patterns requiring explanation". Traps of that kind stayed in the source; everything below is
design rationale and project history, which did not.

Read this if you are reviewing the branch and wonder why something was done the way it was.

## What the mode is

A simplified video-calling surface for someone who cannot operate the full Signal app — set up by a
relative on a shared tablet, not a lockdown or kiosk mode. It replaces the normal main screen with
one tile per contact, routes a tap into Signal's own call lobby, and offers exactly one way back
out: either the Signal PIN or a hidden tap pattern.

The mode intercepts `MainActivity` only. Notifications, share intents and other entry points still
lead into the normal app; this is a simplification, not a restriction.

## Call screen

**The drag handle is gone, not just the drag.** Dragging the sheet open only reveals the call info,
which the mode does not show. Leaving the handle in place would be a promise the sheet no longer
keeps, so both go together.

**All microphone indicators are Signal's own.** The mode once drew a larger copy of the caller's
level in the volume column, behind a "Microphone level" setting, and suppressed Signal's stock
indicators to make room for it. Testing on a real tablet (2026-10-12) removed the whole idea: the
copy duplicated the indicator in the self preview, the partner's indicator bottom left had been
suppressed by mistake (it answers a different question — whether the other side is being heard),
and in the lobby the copy could never move, because 1:1 calls only report audio levels once the
call is connected. Now the self preview shows the own microphone and the slot bottom left shows
the partner, both exactly as in stock Signal.

The volume buttons are present from the lobby onwards. Before the call connects they are invisible
rather than absent (`showVolume`): there is nothing to turn up yet, but they hold their place so
the group does not shift when the call starts.

**The camera switch sits in the control strip.** Signal's own switch is in the corner of the small
self preview, which is too small a target here. Showing both would be two icons for one action, so
the corner one is suppressed while the mode is on.

**While it rings, the lobby's picture stays.** Avatar and name centred, status beneath, reusing
`PreJoinHeader`. Signal's own bar puts both in the top left corner, which moves them the moment the
user taps the call button.

**The volume controls fade with the rest of the controls**, driven by the same sheet state the top
bar watches, rather than on a schedule of their own. With "Always show call controls" off, all of it
disappears together.

**Colours come from Signal's own call resources, not from new ones.** "Start Video Call" uses
`webrtc_answer_background` (#34C759), the same resource as `AcceptCallButton` and the "Answer"
button; the back button in the lobby uses `webrtc_hangup_background` (#F07168), the same one as
`EndVideoCallButton`, `HangupButton` and "Decline". The lobby offers the same pair of decisions one
step earlier — place the call or do not — and Signal already colours that pair consistently. Earlier
drafts used `signal_light_colorPrimary` ("some primary button") and a literal `Color.Red`, which sat
visibly beside Signal's red rather than within it.

## Incoming calls

**No back arrow on the answer screen.** `onNavigateUpClicked` calls `onBackPressed`, and
`WebRtcCallActivity` deliberately does not enter picture-in-picture for `CALL_INCOMING` — it
finishes the activity. The answer screen would vanish while the phone kept ringing, and because the
mode puts the notification on a quiet channel there would be no banner to get back to it.

**No CallStyle banner while the app is already in front.** Signal starts the call screen itself
(`IncomingCallActionProcessor` → `startWebRtcCallActivityIfPossible`); the full-screen intent is
only the fallback for not being in the foreground. On a locked or backgrounded device nothing
changes.

## Settings

**Tap count and tap window are validated as a pair.** Each value is reasonable on its own, but
"10 taps in 1 second" is not reachable by hand. With the PIN method off, the gesture is the only way
out, so an unreachable pair locks the caregiver out of the device for good. Both setters clamp the
pair, the list does not offer unreachable combinations, and the gesture reads the effective values
so a stored pair from an older build cannot survive until the settings are next opened.

**The screen sharing row shows a notice when the switch cannot work.** `android.calling.screenSharing`
is a server-side flag. Rather than omitting the row, the same condition swaps the switch for a short
explanation — otherwise the caregiver looks for a row that every guide mentions and that is simply
missing here.

## Strings

All mode strings live in `res/values/strings_accessibility.xml`, not in Signal's `strings.xml`.
Signal's file comes from Transifex and is overwritten on every rebase. For the same reason the mode
does not borrow Signal's own string keys.

The fork additionally ships `res/values-de/strings_accessibility.xml`, because the device this was
built for is German. That file is not part of the upstream-facing branch: Signal translates through
Transifex, not through hand-written translation files in pull requests.
