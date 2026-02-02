# Requirements - HyArena v2

## Question 1: Core Purpose

What is the main thing players do in this plugin?

**Answer:** Players join a social hub in the default world. From there they can queue for certain arenas which can be scattered across any subset of other worlds.

---

## Question 2: Arena Game Modes

When a player enters an arena, what happens? How do they win?

**Answer:** Each arena defines a game mode. The match runs by that game mode's ruleset until a win condition is met.

---

## Question 3: Which Game Modes?

What specific game modes do you want to support at launch?

**Answer:**
1. **Duel** - 1v1, last standing wins
2. **Deathmatch** - FFA, first to reach kill target wins
3. **Timed Deathmatch** - FFA with time limit, most kills when time runs out wins

---

## Question 4: Bots

Should arenas support AI bots as opponents? If yes, when are they used?

**Answer:** Yes. Bots fill empty slots until the minimum player count needed to start the match is reached.

---

## Question 5: Respawning

When a player dies, what happens?

**Answer:** Depends on game mode. If the game mode supports respawns, player respawns at a random spawn point inside the arena. If not, they're eliminated and sent back to the hub.

---

## Question 6: Kits / Loadouts

Do players get equipment when entering an arena?

**Answer:** Yes. Players have nothing outside of matches. They receive a kit when the match starts.

---

## Question 7: Kit Selection

How is the kit determined?

**Answer:** Kits are defined globally. Each arena flags which kits are allowed/available for that arena.

---

## Question 8: Stats & Persistence

Should the plugin track player stats (kills, deaths, wins, etc.)?

**Answer:** Yes. Stats should distinguish between:
- Player vs Player (PvP kills, deaths, etc.)
- Player vs Bot (bot kills, deaths by bot, etc.)

---

## Question 9: Stats Storage

Where should stats be stored?

**Answer:** Transmitted to an external web API server which processes stats and generates leaderboards, profiles, etc.

---

## Question 10: Player Interface

How do players interact with the system (queue, select kit, etc.)?

**Answer:** Interactable blocks in the hub world open UI menus. Players navigate through UI to queue, select kits, etc.

---

## Question 11: In-Match HUD

What information should players see during a match?

**Answer:**
- Time remaining
- Player list with names
- Kills per player
- Deaths per player
- Score (optional, game mode dependent)

---

## Question 12: Hub/Queue HUD

Should players see any HUD while in the hub or while queuing?

**Answer:** Yes, similar to current implementation:
- Players online
- Players in queue
- Players in active matches
- Queue status/position when queuing

---

## Question 13: Boundaries & Protection

What restrictions apply to players?

Examples:
- "Can't leave hub area"
- "Can't leave arena during match"
- "Can't break/place blocks"
- "No PvP in hub"

**Answer:**
- Players in hub cannot be damaged (no PvP in hub)
- Only players in ongoing matches can damage each other
- No player can break or place blocks unless they have a certain permission

---

## Question 14: Arena Availability

How do arenas become available for matches?

Options:
- **Single instance**: Each arena can only host one match at a time
- **Multiple instances**: Same arena config can spawn multiple concurrent matches
- **Both**: Some arenas single-instance, some multi-instance

**Answer:** Single instance. Each arena can only host one match at a time.

---

## Question 15: Match Start Conditions

When does a match actually start?

Options:
- **Immediately**: As soon as minimum players are in queue, teleport and start
- **Countdown**: Once minimum reached, show countdown (e.g., 10 seconds) then start
- **Manual**: Admin/player triggers the start
- **Hybrid**: Countdown, but can be skipped if all players ready up

**Answer:** Countdown. Once minimum players reached, wait a configurable time for more players to join, then start automatically.

---

## Question 16: Player Disconnect During Match

What happens if a player disconnects during an active match?

Options:
- **Forfeit**: Player loses, match continues/ends based on remaining players
- **Replace with bot**: Bot takes over the player's slot
- **Pause**: Match pauses, waits for reconnect (with timeout)
- **Nothing special**: Slot just becomes empty, match continues

**Answer:** Forfeit. Player loses, match continues or ends based on remaining players.

---

## Question 17: Spectating

Can players spectate ongoing matches?

Options:
- **No**: No spectating
- **Yes, anyone**: Any player can spectate any match
- **Yes, after elimination**: Eliminated players can spectate remainder of their match
- **Yes, both**: Eliminated players auto-spectate, others can join too

**Answer:** No spectating for initial version. May be added later.

---

## Question 18: Admin Controls

What admin capabilities are needed?

Examples:
- Force-start/stop matches
- Kick player from match
- Ban player from arenas
- Reload configs without restart
- Create/edit arenas in-game

Which of these (or others) do you need?

**Answer:**
- Create/edit arenas in-game
- Edit hub settings
- Create/edit kits

---

## Question 19: Commands vs UI for Admin

How should admins access these controls?

Options:
- **Commands only**: Chat commands like `/arena create`, `/kit edit`
- **UI only**: Admin menu opened via block/item interaction
- **Both**: Commands and UI available

**Answer:** Single command `/arena` opens UI. In-game statues (interactable blocks) also trigger the same UI. Admins see additional options in the UI based on permissions.

---

## Question 20: Queue Behavior

When a player queues, what happens?

Options:
- **Stay in hub**: Player remains in hub, can walk around while waiting
- **Waiting room**: Player teleported to a waiting area until match starts
- **Frozen in place**: Player can't move while in queue

**Answer:** Stay in hub. Player remains in hub, can walk around while waiting.

---

## Question 21: Multiple Queues

Can a player queue for multiple arena types at once?

Options:
- **No**: One queue at a time
- **Yes**: Can queue for multiple, joins first available match

**Answer:** No. One queue at a time.

---

## Question 22: Party/Group System

Can players queue together as a group/party?

Options:
- **No**: Solo queue only
- **Yes**: Players can form parties and queue together

**Answer:** No. Solo queue only.

---

## Question 23: Match End Behavior

After a match ends, what happens to players?

Options:
- **Instant teleport**: Immediately back to hub
- **Victory screen**: Show winner/stats for a few seconds, then teleport
- **Player choice**: Button to leave or stay and spectate arena

**Answer:** Victory screen. Show winner/stats for a few seconds, then teleport to hub.

---

## Question 24: Kit Contents

What can a kit contain?

Options:
- **Items only**: Weapons, armor, consumables
- **Items + effects**: Items plus potion effects, buffs
- **Items + effects + abilities**: Special abilities tied to the kit
- **Full customization**: Items, effects, abilities, and attribute modifiers (health, speed, etc.)

**Answer:** Items only (like current implementation):
- Hotbar items (with optional quantity, e.g., "arrow:64")
- Armor pieces (helmet, chest, legs, hands)
- Offhand item

---

## Question 25: Bot Difficulty

Should bots have configurable difficulty levels?

Options:
- **No**: Single difficulty, bots just fill slots
- **Yes, per-arena**: Each arena defines bot difficulty
- **Yes, global levels**: Define difficulty presets (Easy, Medium, Hard) used across arenas

**Answer:** Yes, per-arena. Each arena defines its own bot difficulty.

---

## Question 26: Rewards

Should players receive rewards for winning/participating?

Options:
- **No**: No in-game rewards, stats only
- **Yes, currency**: Earn currency to spend on cosmetics/unlocks
- **Yes, items**: Receive items after matches
- **External only**: Rewards handled by external API/system

**Answer:** Yes, currency. Players earn currency to spend on cosmetics/unlocks.

---

## Question 27: Currency Storage

Where should player currency be stored?

Options:
- **Local**: Plugin stores currency in local files/database
- **External API**: Currency managed by external web API (same as stats)
- **Both**: Local cache with sync to external API

**Answer:** External API. Currency managed by external web API (same as stats).

---

## Question 28: What Can Currency Buy?

What can players spend currency on?

Examples:
- Cosmetic skins/models
- New kits (unlock access)
- Titles/prefixes
- Hub perks (trails, particles)

Which of these (or others)?

**Answer:** All of the above:
- Cosmetic skins/models
- New kits (unlock access)
- Titles/prefixes
- Hub perks (trails, particles)

---

## Question 29: Chat Integration

Should the plugin integrate with chat?

Options:
- **No**: No chat features
- **Match chat only**: Players in same match can chat with each other
- **Global + match**: Global hub chat plus match-specific chat
- **Full system**: Global, match, and private messaging

**Answer:** Full system. Global chat, match chat, and private messaging.

---

## Question 30: Sound Effects

Should the plugin play sound effects for events?

Examples:
- Match start countdown beeps
- Kill sounds
- Victory/defeat jingles
- Queue pop notification

Options:
- **No**: No custom sounds
- **Yes, minimal**: Key events only (match start, end)
- **Yes, full**: Sounds for most events

**Answer:** Yes, full. Sounds for most events.

---

## Question 31: Localization

Should text/messages support multiple languages?

Options:
- **No**: English only
- **Yes**: Support multiple languages via language files

**Answer:** No. English only.

---

## Question 32: Anti-Cheat

Should the plugin include any anti-cheat measures?

Options:
- **No**: Rely on external anti-cheat or none
- **Basic**: Simple checks (speed, fly, reach distance)
- **Integrated**: Hook into existing anti-cheat system

**Answer:** Basic. Simple checks (speed, fly, reach distance).

---

## Question 33: Logging & Debug

What level of logging/debugging is needed?

Options:
- **Minimal**: Errors only
- **Standard**: Errors + important events (match start/end, player join/leave)
- **Verbose**: Full debug logging with config toggle

**Answer:** Verbose. Full debug logging with config toggle.

---

## Question 34: Server Restart Handling

What happens to active matches when the server restarts/reloads?

Options:
- **Lost**: Matches end, players return to hub on rejoin
- **Saved**: Match state persisted, can resume after restart
- **Graceful**: Warn players, wait for matches to end before restart

**Answer:** Lost. Matches end, players return to hub on rejoin.

---

## Question 35: Configuration Format

What format should config files use?

Options:
- **JSON**: Like current implementation
- **YAML**: More human-readable, supports comments
- **TOML**: Simple, clean syntax

**Answer:** JSON. Like current implementation. Most editing happens through UI anyway.

---

## Question 36: Additional Features

Is there anything else not covered that you want in the initial version?

Examples of things we haven't discussed:
- Seasonal events / limited-time modes
- Achievements system
- Daily/weekly challenges
- Friends list integration
- Replay system

**Answer:** Yes, include:
- Seasonal events / limited-time modes
- Achievements system
- Daily/weekly challenges
- Friends list integration

---

## Question 37: Final Check

Looking at the full requirements, is there anything you want to change or clarify before we move to the architecture design phase?

Summary of what we're building:
- Hub in default world, arenas in other worlds
- 3 game modes: Duel (1v1), Deathmatch (FFA), Timed Deathmatch
- **Game modes must be easily extendable** (pluggable architecture)
- Bots fill empty slots (per-arena difficulty)
- Global kits with arena whitelisting
- Stats + currency via external API
- Shop: skins, kits, titles, hub perks
- Single `/arena` command opens UI (also via hub statues)
- Full chat system (global, match, private)
- Full sound effects
- Basic anti-cheat
- Verbose logging
- Achievements, daily/weekly challenges, seasonal events, friends list

**Answer:** Game modes must be easily extendable (pluggable architecture). Otherwise, requirements are complete.

---

# Requirements Complete

Ready to proceed to architecture design phase.
