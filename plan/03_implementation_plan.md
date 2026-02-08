# Implementation Plan - HyArena v2

## Phased Approach

Build in layers - each phase produces a working (but incomplete) plugin.

---

## Phase 1: Core Foundation - COMPLETE
*Get players joining and moving between worlds*

- [x] Project setup (package structure, dependencies)
- [x] `ConfigManager` - load global.json, hub.json
- [x] `EventBus` - basic pub/sub
- [x] `HubManager` - spawn players in hub
- [x] `/arena` command (opens placeholder UI)
- [x] Basic boundary system (keep players in hub)
- [x] `PlayerReadyEvent` handling (world change detection)

**Milestone**: Players join, spawn in hub, can't leave hub area.

---

## Phase 2: Arena & Match Basics - COMPLETE
*Create and run a simple match*

- [x] `Arena` class with config loading
- [x] `Match` class with state machine
- [x] `MatchManager` - create/track matches
- [x] `GameMode` interface + `DuelGameMode`
- [x] `Participant` interface + `PlayerParticipant`
- [x] Basic teleportation (hub ↔ arena)
- [x] Match lifecycle events

**Milestone**: Two players can teleport to arena, fight, winner returns to hub.

---

## Phase 3: Queue System - COMPLETE
*Proper matchmaking flow*

- [x] `QueueManager` - join/leave queue
- [x] `Matchmaker` - countdown logic, match creation
- [x] `QueueHud` - show queue status
- [x] `LobbyHud` - show server stats
- [x] Queue events (PlayerQueuedEvent, etc.)

**Milestone**: Players queue via UI, match starts automatically after countdown.

---

## Phase 4: Kits & Combat - COMPLETE
*Equipment and proper fighting*

- [x] `Kit` class with config loading
- [x] `KitManager` - apply kits to players
- [x] Kit selection in UI
- [x] Damage detection system
- [x] Kill/death tracking per match
- [x] Respawn logic (game mode dependent)

**Milestone**: Players get kits, kills are tracked, respawns work.

---

## Phase 5: Bots - COMPLETE
*AI opponents*

- [x] `BotParticipant` implementation
- [x] `BotAI` - targeting, movement, attacking
- [x] `BotDifficulty` settings
- [x] Bot spawning in matches
- [x] Bot kit application
- [x] Cross-world bot support (world-thread-local management)

**Milestone**: Bots fill empty slots, fight players, work in any world.

---

## Phase 6: HUDs, UI & Game Modes - COMPLETE
*Complete user interface and all game modes*

- [x] `MatchHud` - scoreboard, timer
- [x] Victory screen (interactive page with close button)
- [x] Full UI page hierarchy (MainMenu → subpages)
- [x] Arena list & detail pages
- [x] Admin pages (arena/kit CRUD, hub settings, /hyadmin command)
- [x] Game modes: `duel`, `lms`, `deathmatch`, `koth`, `kit_roulette`
- [x] KOTH score HUD

**Milestone**: Full UI flow working, HUDs show all info, all game modes implemented.

---

## Phase 7: Economy & Shop
*Currency and purchases*

- [ ] Currency display in UI
- [ ] Shop pages (kits, cosmetics, perks)
- [ ] Purchase flow with API
- [ ] Unlock checking (kit access, etc.)

**Milestone**: Players earn currency, buy items from shop.

---

## Phase 8: Achievements & Challenges
*Progression systems*

- [ ] `AchievementTracker` - event subscriptions
- [ ] Achievement definitions (config or API)
- [ ] Challenge system (daily/weekly)
- [ ] Achievement/challenge UI pages
- [ ] Progress tracking via API

**Milestone**: Achievements unlock, challenges track progress.

---

## Phase 9: Polish & Extras
*Final features*

- [ ] `SoundManager` - all sound effects
- [ ] `AntiCheat` - basic checks
- [ ] Seasonal events framework
- [ ] Logging system (verbose toggle)
- [ ] Performance optimization
- [ ] Testing & bug fixes

**Milestone**: Production-ready plugin.

---

## Phase 10: External API & Website Overhaul
*Stats, persistence, and website redesign*

- [ ] `ApiClient` implementation
- [ ] Stats recording on match end
- [ ] Player stats display in UI
- [ ] Leaderboard integration
- [ ] Website overhaul (details TBD)

**Milestone**: Match results sent to API, stats viewable, website updated.

---

## Implementation Approach

**Answer:** Phase by phase with testing checkpoints.

Each phase will:
1. Implement all features for that phase
2. Provide testing instructions
3. Wait for confirmation before moving to next phase

This ensures each layer works correctly before building on top of it.
