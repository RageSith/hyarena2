# KNOWN BUGS

These is the current Bug-List. When Bugs are noticed the user will update the BUGS PRESENT section.
Once a Bug got fixed, move it to the Bug fixed Section with an short explaination what caused the 
bug and what solved it. 

**IMPORTANT:** YOU DONT NEED TO FIX BUGS HERE ON YOUR OWN, THE USER WILL PROMPT YOU WHEN TO DO!

## BUGS PRESENT

### Match / Gamerelated

#### Real Death under unknown circumstances

Me as a player die from time to time a real engine death which should not happen and therefore i got 
send to the arenas world spawn. This happened to me only in the KOTH mode so far. 

## BUGS FIXED

### Kit Manager — Items not persisting on save

**Cause:** In `KitEditorPage.java`, the item text field event binding used `append("Value", ...)` instead of `append("@Value", ...)`. Without the `@` prefix, Hytale sends the literal selector string instead of resolving the actual field value, and the key doesn't match the codec's `"@Value"` key — so `data.value` was always null.

**Fix:** Changed `"Value"` to `"@Value"` on line 125 of KitEditorPage.java.