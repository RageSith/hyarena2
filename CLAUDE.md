# HyArena2 - Claude Context

## Project Overview

HyArena2 is a PvP arena plugin for Hytale servers. This is a complete rewrite with clean architecture designed from scratch.

## Important Guidelines (2026)

1. **We're in 2026** - Hytale is released with real modding/server APIs
2. **Use internet research** - When unsure about Hytale server/modding implementation details, use web search to find current documentation, forums, or examples. Don't rely on outdated or assumed knowledge.
3. **Don't fabricate APIs** - If even after internet research you cannot find how to implement something in Hytale, report back to the user and wait for guidance rather than making things up

## Planning Documents

All design decisions are documented in `/plan/`:
- `01_requirements.md` - 37 answered requirement questions
- `02_architecture.md` - 16 architecture decisions
- `03_implementation_plan.md` - 12-phase implementation roadmap

**Read these before making changes** - they define how the plugin should work.

## Reference Project

The original HyArena project at `/home/hypanel/HyArena/` contains working examples of:
- Hytale API usage (players, worlds, entities, teleportation)
- Multi-world handling (Universe.get().getWorld())
- NPC/Bot spawning and AI
- Custom UI pages and HUDs
- Damage detection systems
- Config loading with Gson
- Event handling (PlayerReadyEvent, PlayerDisconnectEvent)
- Cross-world teleportation with delays
- Boundary enforcement

**Use it as reference for Hytale API patterns**, but don't copy its architecture - it has technical debt we're avoiding.

Key reference files in old project:
- `src/main/java/de/ragesith/plugin/HyArena.java` - Main plugin, event handling
- `src/main/java/de/ragesith/plugin/bot/BotManager.java` - Bot spawning
- `src/main/java/de/ragesith/plugin/hub/HubManager.java` - Teleportation
- `src/main/java/de/ragesith/plugin/arena/Match.java` - Match logic
- `src/main/java/de/ragesith/plugin/ui/*.java` - UI examples
- `docs/MULTIWORLD_GUIDE.md` - Critical multi-world lessons

## Current Progress

### Phase 1: Core Foundation - COMPLETE
- [x] Project setup
- [x] `ConfigManager` - load global.json, hub.json
- [x] `EventBus` - basic pub/sub
- [x] `HubManager` - spawn players in hub
- [x] `/arena` command (placeholder UI)
- [x] Basic boundary system
- [x] `PlayerReadyEvent` handling (world change detection)

### Phase 2: Arena & Match Basics - COMPLETE
- [x] Arena configuration system (ArenaConfig, Arena)
- [x] Match state machine (WAITING → STARTING → IN_PROGRESS → ENDING → FINISHED)
- [x] GameMode interface + DuelGameMode (1v1 last standing)
- [x] Participant abstraction (Participant, PlayerParticipant)
- [x] KillDetectionSystem - damage/kill tracking, prevents non-match damage
- [x] Match events (created, started, ended, finished)
- [x] Participant events (joined, left, damaged, killed)
- [x] Multi-world support (test_duel in default, the_pit in world2)
- [x] Thread-safe ticking via world.execute()
- [x] Test commands: /tmarenas, /tmcreate, /tmjoin, /tmstart, /tmlist, /tmleave, /tmcancel

**Phase 2 polish TODOs:**
- [x] Freeze player movement during STARTING countdown
- [x] Spawn immunity period (3s) when match begins
- [x] Spawn point rotation (yaw/pitch already saved in config, applied on teleport)
- [x] Test full 2-player match flow (kill → winner → teleport back)

### Phase 3: Queue System - COMPLETE
- [x] Queue events (PlayerQueuedEvent, PlayerLeftQueueEvent, QueueMatchFoundEvent)
- [x] QueueEntry data class
- [x] QueueManager - per-arena queues, join/leave, cooldowns, hub-only validation
- [x] Matchmaker - periodic tick, countdown timers, auto-match creation
- [x] Combined LobbyHud - top bar (server stats) + bottom bar (queue info, auto-shown when in queue)
- [x] HudManager - tracks LobbyHud lifecycle
- [x] EmptyHud - placeholder for clearing HUDs
- [x] Test commands: /tqjoin <arenaId>, /tqleave
- [x] isArenaInUse() method in MatchManager
- [x] Integration in HyArena2.java (scheduler, event subscriptions, disconnect cleanup)

**Queue system test commands:**
- `/tqjoin <arenaId> [kitId]` - Join queue for an arena with optional kit selection (from hub only)
- `/tqleave` - Leave current queue

### Phase 4: Kits & Combat - COMPLETE
- [x] KitConfig data class (id, displayName, description, permission, items, armor, offhand)
- [x] KitManager - load kits from config/kits/*.json, apply/clear kits
- [x] KitAccessInfo helper class for UI (kit + isUnlocked flag)
- [x] Kit events (KitSelectedEvent, KitAppliedEvent)
- [x] QueueEntry - added selectedKitId field
- [x] Participant interface - added getSelectedKitId(), setSelectedKitId()
- [x] QueueManager - kit validation on queue join, INVALID_KIT result
- [x] Matchmaker - passes kit from queue entry to participant
- [x] Match - applies kit after teleport/heal, clears kit before return to hub
- [x] MatchManager - passes KitManager to Match for kit operations
- [x] Sample kit configs: warrior.json, archer.json, rogue.json
- [x] Test commands: /tkits, /tkit <kitId>, updated /tqjoin

**Kit system test commands:**
- `/tkits` - List all available kits
- `/tkit <kitId>` - Apply a kit to yourself (for testing)
- `/tqjoin <arenaId> [kitId]` - Queue with optional kit selection

### Phase 5: Bots - COMPLETE
- [x] BotDifficulty enum (EASY, MEDIUM, HARD with parameters: reaction time, accuracy, ranges, health multiplier, etc.)
- [x] BotParticipant class implementing Participant interface
- [x] BotAI state machine (IDLE, PATROL, CHASE, ATTACK, RETREAT, DEAD)
- [x] BotManager - spawn/despawn/tick lifecycle, entity UUID tracking, damage handling
- [x] Match.java - addBot() method, bot cleanup in finish/cancel
- [x] MatchManager - setBotManager(), addBotToMatch()
- [x] KillDetectionSystem - handles both player and bot victims/attackers
- [x] ArenaConfig - added botModelId, autoFillEnabled, autoFillDelaySeconds, minRealPlayers
- [x] Matchmaker - auto-fill logic with configurable delay, fills remaining slots with bots
- [x] HyArena2.java - BotManager initialization, bot ticker, test commands
- [x] Test commands: /tbot, /tbotlist, /tbotremove

**Bot system test commands:**
- `/tbot [difficulty] [kitId]` - Spawn a bot in your current match
- `/tbotlist` - List all active bots with health/status
- `/tbotremove <botName>` - Remove a specific bot

**Bot difficulty parameters:**
| Parameter | EASY | MEDIUM | HARD |
|-----------|------|--------|------|
| Reaction Time | 1500ms | 800ms | 300ms |
| Aim Accuracy | 40% | 65% | 90% |
| Attack Range | 3.0 | 4.0 | 5.0 |
| Chase Range | 15.0 | 25.0 | 40.0 |
| Movement Speed | 0.8x | 1.0x | 1.2x |
| Health Multiplier | 0.8x | 1.0x | 1.4x |
| Attack Cooldown | 1000ms | 900ms | 600ms |
| Retreat Threshold | 30% | 25% | 15% |

**Auto-fill configuration (per arena):**
| Field | Default | Description |
|-------|---------|-------------|
| `autoFillEnabled` | false | Enable bot auto-fill for this arena |
| `autoFillDelaySeconds` | 30 | Wait time before filling with bots |
| `minRealPlayers` | 1 | Minimum real players needed before auto-fill |
| `botDifficulty` | MEDIUM | Difficulty level for auto-filled bots |
| `botModelId` | null | NPC model for bots (required for bot spawning) |

**Auto-fill flow:**
1. Player joins queue → auto-fill timer starts
2. After `autoFillDelaySeconds`, if `>= minRealPlayers` in queue:
   - Match is created with queued players
   - Remaining slots filled with bots at configured difficulty
3. Normal queue behavior still applies (instant start at maxPlayers, countdown at minPlayers)

**Phase 5 polish (completed during testing):**
- [x] Bot freeze during countdown (teleport-back approach - freeze effects don't work on NPC AI)
- [x] Bot despawn on death in no-respawn game modes
- [x] Kit fallback to first allowed kit when player has none selected
- [x] Damage notifications (attacker sees "-X / VictimName", victim sees "-X / AttackerName")
- [x] LobbyHud shows after returning from same-world arenas (explicit call in teleport callback)
- [x] Queue status format: "Waiting for players (Xs)" shows auto-fill timer in parentheses
- [x] 3-second match ready countdown before teleport (prevents arena overlap race condition)
- [x] Last hit tracking for environmental deaths (10s window for kill attribution)

### Phase 6: HUDs & UI Polish - NOT STARTED
Next phase focuses on complete user interface:
- [ ] `MatchHud` - scoreboard showing kills/deaths, match timer
- [ ] Victory screen overlay
- [ ] Full UI page hierarchy (MainMenu → Arena List → Arena Detail → Queue)
- [ ] Admin pages (arena editor, kit editor, hub settings)

**Milestone**: Full UI flow working, HUDs show all match info.

### Phases 7-12: Not Started
See `plan/03_implementation_plan.md` for full roadmap.

## Package Structure

```
de.ragesith.hyarena2/
  arena/          # Arena, Match, Spawn points
  bot/            # Bot AI, BotParticipant, BotManager
  gamemode/       # GameMode interface + implementations
  queue/          # Queue, Matchmaker
  hub/            # Hub management, boundaries
  participant/    # Player/Bot abstraction
  kit/            # Kit definitions, application
  shop/           # Currency, purchases, unlocks
  chat/           # Chat channels, messaging
  api/            # External API client
  ui/             # All UI pages and HUDs
  config/         # Configuration classes
  event/          # Event bus and event definitions
  util/           # Shared utilities
```

## Key Design Rules

1. **World-Independent Core** - Match logic doesn't care which world it runs in
2. **Use Universe API** - `Universe.get().getWorld(name)` for world access
3. **Never Store Player Long-Term** - Always get fresh references
4. **Execute on World Thread** - All entity operations via `world.execute()`
5. **Teleport Delays** - 1.5s cross-world, 1s post-combat
6. **Event-Driven** - Components communicate via EventBus
7. **Pluggable Game Modes** - Easy to add new modes
8. **Use Permissions Class** - Always use `Permissions.*` constants, never hardcode permission strings

## Permissions Reference

All permissions are defined in `Permissions.java`. Use these constants throughout the codebase:

| Category | Constant | Node | Use When |
|----------|----------|------|----------|
| **Player** | `PLAYER` | `hyarena.player` | /arena command access |
| | `QUEUE` | `hyarena.queue` | Joining queues |
| | `CHAT` | `hyarena.chat` | Match chat |
| **Bypass** | `BYPASS_BOUNDARY` | `hyarena.bypass.boundary` | Skip boundary checks |
| | `BYPASS_COOLDOWN` | `hyarena.bypass.cooldown` | Skip queue cooldowns |
| | `BYPASS_KIT` | `hyarena.bypass.kit` | Access restricted kits |
| **Admin** | `ADMIN` | `hyarena.admin` | Admin commands |
| | `ADMIN_ARENA` | `hyarena.admin.arena` | Arena editing |
| | `ADMIN_KIT` | `hyarena.admin.kit` | Kit editing |
| | `ADMIN_HUB` | `hyarena.admin.hub` | Hub settings |
| | `ADMIN_RELOAD` | `hyarena.admin.reload` | Config reload |
| | `ADMIN_MATCH` | `hyarena.admin.match` | Force start/end |
| | `ADMIN_BOT` | `hyarena.admin.bot` | Bot management |
| **Mod** | `MOD_KICK` | `hyarena.mod.kick` | Kick players |
| | `MOD_BAN` | `hyarena.mod.ban` | Ban players |
| | `MOD_STATS` | `hyarena.mod.stats` | View stats |
| **VIP** | `VIP_PRIORITY` | `hyarena.vip.priority` | Priority queue |
| | `VIP_ARENA` | `hyarena.vip.arena` | Exclusive arenas |
| | `VIP_KIT` | `hyarena.vip.kit` | Exclusive kits |
| **Debug** | `DEBUG` | `hyarena.debug` | Debug info |

**Usage:** `player.hasPermission(Permissions.ADMIN)` - never use raw strings!

## Build & Deploy

```bash
./gradlew build
```

JAR deploys to: `/home/hypanel/hytale/fdfae6e5-819f-466e-bed3-2feef026eb90/mods/`

## Testing Approach

Each phase produces a testable milestone. Complete testing before moving to next phase.
