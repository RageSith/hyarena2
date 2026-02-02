# HyArena2 - Claude Context

## Project Overview

HyArena2 is a PvP arena plugin for Hytale servers. This is a complete rewrite with clean architecture designed from scratch.

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

### Phase 1: Core Foundation - IN PROGRESS
- [x] Project setup
- [ ] `ConfigManager` - load global.json, hub.json
- [ ] `EventBus` - basic pub/sub
- [ ] `HubManager` - spawn players in hub
- [ ] `/arena` command (placeholder UI)
- [ ] Basic boundary system
- [ ] `PlayerReadyEvent` handling (world change detection)

### Phases 2-12: Not Started
See `plan/03_implementation_plan.md` for full roadmap.

## Package Structure

```
de.ragesith.hyarena2/
  arena/          # Arena, Match, Spawn points
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

## Build & Deploy

```bash
./gradlew build
```

JAR deploys to: `/home/hypanel/hytale/fdfae6e5-819f-466e-bed3-2feef026eb90/mods/`

## Testing Approach

Each phase produces a testable milestone. Complete testing before moving to next phase.
