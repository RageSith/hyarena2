# Implementation Plan - HyArena v2

## Phased Approach

Build in layers - each phase produces a working (but incomplete) plugin.

---

## Phase 1: Core Foundation
*Get players joining and moving between worlds*

- [ ] Project setup (package structure, dependencies)
- [ ] `ConfigManager` - load global.json, hub.json
- [ ] `EventBus` - basic pub/sub
- [ ] `HubManager` - spawn players in hub
- [ ] `/arena` command (opens placeholder UI)
- [ ] Basic boundary system (keep players in hub)
- [ ] `PlayerReadyEvent` handling (world change detection)

**Milestone**: Players join, spawn in hub, can't leave hub area.

---

## Phase 2: Arena & Match Basics
*Create and run a simple match*

- [ ] `Arena` class with config loading
- [ ] `Match` class with state machine
- [ ] `MatchManager` - create/track matches
- [ ] `GameMode` interface + `DuelGameMode`
- [ ] `Participant` interface + `PlayerParticipant`
- [ ] Basic teleportation (hub ↔ arena)
- [ ] Match lifecycle events

**Milestone**: Two players can teleport to arena, fight, winner returns to hub.

---

## Phase 3: Queue System
*Proper matchmaking flow*

- [ ] `QueueManager` - join/leave queue
- [ ] `Matchmaker` - countdown logic, match creation
- [ ] `QueueHud` - show queue status
- [ ] `LobbyHud` - show server stats
- [ ] Queue events (PlayerQueuedEvent, etc.)

**Milestone**: Players queue via UI, match starts automatically after countdown.

---

## Phase 4: Kits & Combat
*Equipment and proper fighting*

- [ ] `Kit` class with config loading
- [ ] `KitManager` - apply kits to players
- [ ] Kit selection in UI
- [ ] Damage detection system
- [ ] Kill/death tracking per match
- [ ] Respawn logic (game mode dependent)

**Milestone**: Players get kits, kills are tracked, respawns work.

---

## Phase 5: Bots
*AI opponents*

- [ ] `BotParticipant` implementation
- [ ] `BotAI` - targeting, movement, attacking
- [ ] `BotDifficulty` settings
- [ ] Bot spawning in matches
- [ ] Bot kit application

**Milestone**: Bots fill empty slots, fight players, work in any world.

---

## Phase 6: HUDs & UI Polish
*Complete user interface*

- [ ] `MatchHud` - scoreboard, timer
- [ ] Victory screen
- [ ] Full UI page hierarchy (MainMenu → subpages)
- [ ] Arena list & detail pages
- [ ] Admin pages (arena/kit/hub editor)

**Milestone**: Full UI flow working, HUDs show all info.

---

## Phase 7: External API Integration
*Stats and persistence*

- [ ] `ApiClient` implementation
- [ ] Stats recording on match end
- [ ] Player stats display in UI
- [ ] Leaderboard integration

**Milestone**: Match results sent to API, stats viewable.

---

## Phase 8: Economy & Shop
*Currency and purchases*

- [ ] Currency display in UI
- [ ] Shop pages (kits, cosmetics, perks)
- [ ] Purchase flow with API
- [ ] Unlock checking (kit access, etc.)

**Milestone**: Players earn currency, buy items from shop.

---

## Phase 9: Social Features
*Chat and friends*

- [ ] `ChatManager` - global/match/private channels
- [ ] Chat commands (/m, /msg)
- [ ] Friends list (via API)
- [ ] Friend UI page

**Milestone**: Full chat system, friends list working.

---

## Phase 10: Achievements & Challenges
*Progression systems*

- [ ] `AchievementTracker` - event subscriptions
- [ ] Achievement definitions (config or API)
- [ ] Challenge system (daily/weekly)
- [ ] Achievement/challenge UI pages
- [ ] Progress tracking via API

**Milestone**: Achievements unlock, challenges track progress.

---

## Phase 11: Additional Game Modes
*Extend gameplay variety*

- [ ] `DeathmatchGameMode` (FFA, first to X kills)
- [ ] `TimedDeathmatchGameMode` (FFA, most kills in time)
- [ ] Game mode configs
- [ ] Game mode selection in arena config

**Milestone**: All three game modes playable.

---

## Phase 12: Polish & Extras
*Final features*

- [ ] `SoundManager` - all sound effects
- [ ] `AntiCheat` - basic checks
- [ ] Seasonal events framework
- [ ] Logging system (verbose toggle)
- [ ] Performance optimization
- [ ] Testing & bug fixes

**Milestone**: Production-ready plugin.

---

## Implementation Approach

**Answer:** Phase by phase with testing checkpoints.

Each phase will:
1. Implement all features for that phase
2. Provide testing instructions
3. Wait for confirmation before moving to next phase

This ensures each layer works correctly before building on top of it.

---

# Ready to Begin

Start with **Phase 1: Core Foundation** when ready.
