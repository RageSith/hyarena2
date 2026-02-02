# Architecture Design - HyArena v2

## Design Principles

Based on requirements, these principles will guide the architecture:

1. **World-Independent Core** - Match logic doesn't care which world it runs in
2. **Pluggable Game Modes** - Easy to add new game modes without touching core code
3. **Event-Driven** - Components communicate via events, not direct coupling
4. **External API First** - Stats, currency, achievements all flow through API
5. **Single Responsibility** - Each class does one thing well

---

## Question A1: Package Structure

How should we organize the code?

**Option 1: Feature-based** (recommended)
```
de.ragesith.hyarena/
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

**Option 2: Layer-based**
```
de.ragesith.hyarena/
  domain/         # Core business logic
  infrastructure/ # API, config, persistence
  presentation/   # UI, commands, HUDs
  application/    # Services, use cases
```

Which structure do you prefer?

**Answer:** Option 1 - Feature-based structure.

---

## Question A2: Game Mode Interface

How should game modes define their behavior?

```java
public interface GameMode {
    // Identity
    String getId();
    String getDisplayName();

    // Configuration
    int getMinPlayers();
    int getMaxPlayers();
    boolean allowsRespawn();

    // Lifecycle
    void onMatchStart(Match match);
    void onMatchTick(Match match);        // Called every tick
    void onParticipantKill(Match match, Participant killer, Participant victim);
    void onParticipantDeath(Match match, Participant victim);
    void onParticipantLeave(Match match, Participant participant);

    // Win condition
    boolean isMatchOver(Match match);
    List<Participant> getWinners(Match match);

    // Scoring (optional)
    default int getScore(Participant p) { return 0; }
}
```

Does this interface cover everything a game mode needs, or should we add/remove anything?

**Answer:** Yes, this interface covers all needs.

---

## Question A3: Participant Abstraction

Players and bots should be treated uniformly. Here's the proposed interface:

```java
public interface Participant {
    UUID getUuid();
    String getName();
    ParticipantType getType();  // PLAYER or BOT

    // Position & World
    Position getPosition();
    World getWorld();

    // State
    boolean isAlive();
    void setAlive(boolean alive);

    // Match stats (per-match, reset each game)
    int getKills();
    int getDeaths();
    void addKill();
    void addDeath();

    // Actions
    void teleport(Position pos, World world);
    void applyKit(Kit kit);
    void sendMessage(String message);
    void playSound(String soundId);
}
```

`PlayerParticipant` wraps a real player, `BotParticipant` wraps bot AI.

Does this look right?

**Answer:** Yes.

---

## Question A4: Event System

Components should communicate via events to stay decoupled. Core events:

```java
// Match lifecycle
MatchCreatedEvent(Match match)
MatchStartedEvent(Match match)
MatchEndedEvent(Match match, List<Participant> winners)

// Participant events
ParticipantJoinedEvent(Match match, Participant participant)
ParticipantLeftEvent(Match match, Participant participant, LeaveReason reason)
ParticipantKilledEvent(Match match, Participant killer, Participant victim)
ParticipantRespawnedEvent(Match match, Participant participant)

// Queue events
PlayerQueuedEvent(Player player, String arenaTypeId)
PlayerDequeuedEvent(Player player, String arenaTypeId, DequeueReason reason)

// Economy events
CurrencyEarnedEvent(Player player, int amount, String reason)
PurchaseEvent(Player player, String itemId, int cost)

// Achievement events
AchievementUnlockedEvent(Player player, Achievement achievement)
ChallengeCompletedEvent(Player player, Challenge challenge)
```

The API client, UI, sound system, etc. all subscribe to relevant events.

Does this event list cover the needs?

**Answer:** Yes.

---

## Question A5: Match Lifecycle

A match goes through these states:

```
WAITING → STARTING → IN_PROGRESS → ENDING → FINISHED
```

- **WAITING**: Created, waiting for players to be teleported in
- **STARTING**: All players in arena, countdown running
- **IN_PROGRESS**: Active gameplay
- **ENDING**: Win condition met, showing victory screen
- **FINISHED**: Cleanup complete, match can be discarded

The `Match` class manages state transitions:

```java
public class Match {
    private final String id;
    private final Arena arena;
    private final GameMode gameMode;
    private final World world;
    private MatchState state;
    private final List<Participant> participants;

    // State transitions
    public void start();           // WAITING → STARTING
    public void beginGameplay();   // STARTING → IN_PROGRESS
    public void end(List<Participant> winners);  // → ENDING
    public void finish();          // ENDING → FINISHED

    // Called by MatchManager each tick
    public void tick();
}
```

Does this lifecycle make sense?

**Answer:** Yes.

---

## Question A6: Multi-World Architecture

Key lesson from current plugin: world handling must be clean from the start.

**Principle**: Use `Universe.get().getWorld(name)` directly - no custom registry needed.

```java
public class Arena {
    private final String id;
    private final String worldName;
    private final List<SpawnPoint> spawnPoints;
    private final BoundingBox bounds;

    public World getWorld() {
        return Universe.get().getWorld(worldName);
    }
}

public class Match {
    private final Arena arena;

    public World getWorld() {
        return arena.getWorld();
    }

    // All entity operations go through this
    public void executeOnWorld(Runnable action) {
        getWorld().execute(action);
    }
}
```

**Rules enforced by design**:
1. Never store `Player` objects long-term - always get fresh reference
2. All entity operations use `match.executeOnWorld()`
3. Teleports use proper delays (1.5s cross-world, 1s post-combat)

Does this approach address the issues we had?

**Answer:** Yes.

---

## Question A7: External API Client

All persistent data goes through the external API. The client should be:

```java
public class ApiClient {
    // Stats
    CompletableFuture<Void> recordMatchResult(MatchResult result);
    CompletableFuture<PlayerStats> getPlayerStats(UUID playerId);

    // Currency
    CompletableFuture<Integer> getBalance(UUID playerId);
    CompletableFuture<Void> addCurrency(UUID playerId, int amount, String reason);
    CompletableFuture<Boolean> spendCurrency(UUID playerId, int amount, String itemId);

    // Unlocks & Inventory
    CompletableFuture<List<String>> getUnlockedItems(UUID playerId);
    CompletableFuture<Void> unlockItem(UUID playerId, String itemId);

    // Achievements & Challenges
    CompletableFuture<List<Achievement>> getAchievements(UUID playerId);
    CompletableFuture<Void> unlockAchievement(UUID playerId, String achievementId);
    CompletableFuture<List<Challenge>> getActiveChallenges(UUID playerId);
    CompletableFuture<Void> updateChallengeProgress(UUID playerId, String challengeId, int progress);

    // Friends
    CompletableFuture<List<UUID>> getFriends(UUID playerId);
    CompletableFuture<Void> addFriend(UUID playerId, UUID friendId);
    CompletableFuture<Void> removeFriend(UUID playerId, UUID friendId);
}
```

All methods return `CompletableFuture` for async operation. Should anything be added?

**Answer:** No, this covers the needs.

---

## Question A8: Chat System

Three chat channels with clear separation:

```java
public enum ChatChannel {
    GLOBAL,    // All players on server
    MATCH,     // Players in same match only
    PRIVATE    // Direct messages between two players
}

public class ChatManager {
    void sendGlobal(Player sender, String message);
    void sendMatch(Participant sender, Match match, String message);
    void sendPrivate(Player sender, Player recipient, String message);

    // Format messages with player info (titles, colors, etc.)
    String formatMessage(Player player, String message, ChatChannel channel);
}
```

Players switch channels via commands or UI:
- No prefix = global (default)
- `/m <message>` - match chat
- `/msg <player> <message>` - private

Good approach?

**Answer:** Yes. Default (no prefix) sends to global chat.

---

## Question A9: UI Page Structure

All UI flows through the main `/arena` command. Page hierarchy:

```
MainMenu
├── ArenaList          # Browse & queue for arenas
│   └── ArenaDetail    # Arena info, queue button, kit select
├── Shop               # Currency purchases
│   ├── Kits           # Unlock kits
│   ├── Cosmetics      # Skins, titles
│   └── HubPerks       # Trails, particles
├── Profile            # Player stats, achievements
│   ├── Stats          # Detailed statistics
│   ├── Achievements   # Achievement list & progress
│   └── Challenges     # Daily/weekly challenges
├── Friends            # Friends list, invite
├── Settings           # Player preferences
└── Admin (if perm)    # Arena/kit/hub editing
    ├── ArenaEditor
    ├── KitEditor
    └── HubSettings
```

Does this menu structure work?

**Answer:** Yes.

---

## Question A10: HUD System

Two main HUDs that swap based on player state:

**LobbyHud** (shown in hub):
- Players online
- Players in queue
- Active matches count
- Your queue status (if queuing)

**MatchHud** (shown during match):
- Time remaining / elapsed
- Scoreboard (participants, kills, deaths)
- Game mode specific info

```java
public class HudManager {
    void showLobbyHud(Player player);
    void showMatchHud(Player player, Match match);
    void hideAll(Player player);

    // Auto-refresh handled internally with proper state checks
    // HUDs check if player is still valid before each update
}
```

Key safety rules (learned from current plugin):
- Always check player/match state before updating
- Cancel refresh tasks immediately when hiding
- Use `volatile` flags for thread safety

Sound right?

**Answer:** Yes.

---

## Question A11: Bot AI Architecture

Bots need to work properly in multi-world scenarios:

```java
public class BotAI {
    private final BotParticipant bot;
    private final Match match;
    private Participant target;
    private BotDifficulty difficulty;

    // Called on match's world thread
    void tick() {
        if (!bot.isAlive()) return;

        updateTarget();      // Find nearest enemy
        moveTowardTarget();  // Pathfinding
        attemptAttack();     // Attack if in range
    }
}

public enum BotDifficulty {
    EASY(0.5f, 2000, 0.3f),    // slow reactions, long attack delay, low accuracy
    MEDIUM(0.75f, 1000, 0.6f),
    HARD(1.0f, 500, 0.9f);     // fast reactions, quick attacks, high accuracy

    final float moveSpeed;
    final int attackDelayMs;
    final float accuracy;
}
```

**Key rules**:
- Bot ticks always run on the match's world thread
- Bots get fresh player references each tick
- Difficulty is per-arena config

Anything to add?

**Answer:** No.

---

## Question A12: Queue & Matchmaker

Queue is simple (one queue per arena type, no multi-queue):

```java
public class QueueManager {
    private final Map<String, Queue<QueueEntry>> queues;  // arenaTypeId -> queue

    void joinQueue(Player player, String arenaTypeId);
    void leaveQueue(Player player);
    boolean isInQueue(Player player);
    QueueEntry getQueueEntry(Player player);
    int getQueuePosition(Player player);
    int getQueueSize(String arenaTypeId);
}

public class Matchmaker {
    private final Map<String, MatchmakingState> states;  // per arena type

    // Called periodically
    void tick() {
        for (ArenaType arenaType : arenaTypes) {
            MatchmakingState state = states.get(arenaType.getId());

            if (state.isCountdownActive()) {
                // Countdown running - check if time expired or max reached
                if (state.isCountdownExpired() || state.hasMaxPlayers()) {
                    startMatch(arenaType, state);
                }
            } else if (hasMinimumPlayers(arenaType)) {
                // Start countdown
                state.startCountdown(arenaType.getWaitTimeSeconds());
            }
        }
    }

    // Fills remaining slots with bots just before match starts
    void fillWithBots(Match match);
}
```

Flow:
1. Players queue
2. Once minimum reached → start countdown (configurable per arena)
3. During countdown, more players can join
4. When countdown expires OR max players reached → start match (fill with bots if needed)

Correct now?

**Answer:** Yes.

---

## Question A13: Configuration Structure

Config files organized by feature:

```
config/
├── global.json         # Server-wide settings
├── hub.json            # Hub world, spawn, boundaries
├── arenas/
│   ├── arena_1.json    # Individual arena configs
│   └── arena_2.json
├── kits/
│   ├── warrior.json    # Individual kit configs
│   └── archer.json
├── gamemodes/
│   ├── duel.json       # Game mode settings
│   ├── deathmatch.json
│   └── timed_dm.json
└── shop/
    ├── items.json      # Purchasable items
    └── prices.json     # Pricing config
```

Example arena config:
```json
{
  "id": "desert_arena",
  "displayName": "Desert Arena",
  "worldName": "world_arenas",
  "gameMode": "deathmatch",
  "minPlayers": 2,
  "maxPlayers": 8,
  "waitTimeSeconds": 30,
  "botDifficulty": "MEDIUM",
  "allowedKits": ["warrior", "archer"],
  "spawnPoints": [
    {"x": 100, "y": 65, "z": 200, "yaw": 0},
    {"x": 110, "y": 65, "z": 210, "yaw": 180}
  ],
  "bounds": {
    "minX": 50, "maxX": 150,
    "minY": 60, "maxY": 100,
    "minZ": 150, "maxZ": 250
  }
}
```

Does this structure work?

**Answer:** Yes.

---

## Question A14: Anti-Cheat Integration

Basic checks integrated into the match system:

```java
public class AntiCheat {
    // Check thresholds (configurable)
    private float maxSpeed = 10.0f;
    private float maxReach = 5.0f;

    // Called each tick for players in matches
    void checkPlayer(PlayerParticipant player, Match match) {
        checkSpeed(player);
        checkFly(player);
        checkReach(player);
    }

    private void checkSpeed(PlayerParticipant player) {
        float speed = calculateSpeed(player);
        if (speed > maxSpeed) {
            logViolation(player, "SPEED", speed);
            // Optionally: teleport back, warn, or kick
        }
    }

    private void checkReach(PlayerParticipant player) {
        // Check if recent attacks were beyond normal reach
    }
}
```

Violations logged, with configurable actions (warn, teleport back, kick). Good enough for basic?

**Answer:** Yes.

---

## Question A15: Sound & Achievement Systems

Both subscribe to events and react:

```java
public class SoundManager {
    void init(EventBus eventBus) {
        eventBus.subscribe(MatchStartedEvent.class, this::onMatchStart);
        eventBus.subscribe(ParticipantKilledEvent.class, this::onKill);
        eventBus.subscribe(MatchEndedEvent.class, this::onMatchEnd);
        // etc.
    }

    private void onKill(ParticipantKilledEvent event) {
        if (event.killer().getType() == ParticipantType.PLAYER) {
            playSound(event.killer(), "kill_sound");
        }
    }
}

public class AchievementTracker {
    void init(EventBus eventBus) {
        eventBus.subscribe(ParticipantKilledEvent.class, this::trackKill);
        eventBus.subscribe(MatchEndedEvent.class, this::trackWin);
    }

    private void trackKill(ParticipantKilledEvent event) {
        if (event.killer().getType() == ParticipantType.PLAYER) {
            checkAchievements(event.killer(), "kills");
            updateChallengeProgress(event.killer(), "daily_kills");
        }
    }
}
```

Event-driven, decoupled from core logic. Good?

**Answer:** Yes.

---

## Question A16: Final Architecture Check

Here's the complete component overview:

```
┌─────────────────────────────────────────────────────────────┐
│                        HyArena                              │
├─────────────────────────────────────────────────────────────┤
│  Commands: /arena                                           │
│  Interactions: Hub statues                                  │
├─────────────────────────────────────────────────────────────┤
│                      EventBus                               │
│    (all components communicate via events)                  │
├──────────┬──────────┬───────────┬──────────┬───────────────┤
│  Queue   │  Match   │    UI     │   API    │   Systems     │
│  Manager │  Manager │  Manager  │  Client  │               │
├──────────┼──────────┼───────────┼──────────┼───────────────┤
│ -queues  │ -matches │ -pages    │ -stats   │ -SoundManager │
│ -match-  │ -arenas  │ -huds     │ -currency│ -Achievement  │
│  maker   │ -bots    │           │ -friends │ -AntiCheat    │
│          │ -gamemodes│          │ -shop    │ -ChatManager  │
└──────────┴──────────┴───────────┴──────────┴───────────────┘
```

**Key classes**:
- `HyArena` - Main plugin, wires everything
- `EventBus` - Pub/sub event system
- `QueueManager` - Player queues
- `Matchmaker` - Creates matches when ready
- `MatchManager` - Runs active matches
- `Match` - Single match instance
- `GameMode` - Pluggable game rules
- `Participant` - Player/Bot abstraction
- `ApiClient` - External API communication
- `HudManager` - HUD lifecycle
- `ConfigManager` - Config loading

Ready to move to implementation planning, or anything to change?

**Answer:** Ready to proceed.

---

# Architecture Complete

Ready to proceed to implementation planning phase.
