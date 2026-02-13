# KNOWN BUGS

These is the current Bug-List. When Bugs are noticed the user will update the BUGS PRESENT section.
Once a Bug got fixed, move it to the Bug fixed Section with an short explaination what caused the 
bug and what solved it. 

**IMPORTANT:** YOU DONT NEED TO FIX BUGS HERE ON YOUR OWN, THE USER WILL PROMPT YOU WHEN TO DO!

## BUGS PRESENT

### Match / Gamerelated

#### ~~Player boundary causes player to get disconnected after being ported to a match~~ → FIXED, see below

#### Real Death under unknown circumstances

Me as a player die from time to time a real engine death which should not happen and therefore i got 
send to the arenas world spawn. This happened to me only in the KOTH mode so far.

Happens more often now and since the regular vanilla spawn is outside of the arena they spawn
outside the match and cant get in again.

## BUGS FIXED

### Boundary disconnect on match join ("Incorrect teleportId")

**Cause:** Race condition between cross-world teleport and BoundaryManager. When `Match.addPlayer()` was called, the player was registered in `playerToMatch` immediately, but the cross-world teleport takes ~1.5s. During that window, `BoundaryManager.tickArena()` saw the player as "in match" (STARTING state), resolved stale position data, and fired a second teleport to the same position. Two overlapping teleports caused the Hytale client to reject with "Incorrect teleportId" and disconnect. The existing grace period system (`TELEPORT_GRACE_MS = 3000`) was never granted for match-join teleports — only for hub registration and arena→hub returns.

**Fix:** Passed `BoundaryManager` to `Match` (via `MatchManager.setBoundaryManager()`), then called `boundaryManager.grantTeleportGrace(playerUuid)` in `Match.addPlayer()` and `Match.respawnPlayer()` before the teleport starts. The 3s grace window covers the full cross-world delay.

### Kit Manager — Items not persisting on save

**Cause:** In `KitEditorPage.java`, the item text field event binding used `append("Value", ...)` instead of `append("@Value", ...)`. Without the `@` prefix, Hytale sends the literal selector string instead of resolving the actual field value, and the key doesn't match the codec's `"@Value"` key — so `data.value` was always null.

**Fix:** Changed `"Value"` to `"@Value"` on line 125 of KitEditorPage.java.