# Arena Config Reference

All arena configurations are JSON files in `config/arenas/`. This document lists every field, its type, default value, and which game modes use it.

## Core Fields (All Game Modes)

These fields are required for every arena regardless of game mode.

| Field | Type | Default | Required | Description |
|-------|------|---------|----------|-------------|
| `id` | string | — | yes | Unique arena identifier, must match filename |
| `displayName` | string | — | yes | Name shown in UI |
| `worldName` | string | — | yes | Hytale world to host the arena |
| `gameMode` | string | — | yes | Game mode ID (see below) |
| `minPlayers` | int | — | yes | Minimum players to start |
| `maxPlayers` | int | — | yes | Maximum players allowed |
| `waitTimeSeconds` | int | — | yes | Queue countdown duration |
| `matchDurationSeconds` | int | 300 | no | Time limit before match auto-ends |
| `allowedKits` | string[] | null | no | Kit IDs players can pick; null = all kits |
| `spawnPoints` | SpawnPoint[] | — | yes | At least `maxPlayers` entries |
| `bounds` | Bounds | — | yes | Arena boundary AABB |

## Bot / Auto-Fill Fields (All Game Modes)

| Field | Type | Default | Required | Description |
|-------|------|---------|----------|-------------|
| `botDifficulty` | string | null | no | `"EASY"`, `"MEDIUM"`, or `"HARD"` |
| `botModelId` | string | null | no | NPC model for bots (required if bots are used) |
| `autoFillEnabled` | boolean | false | no | Fill remaining slots with bots after timeout |
| `autoFillDelaySeconds` | int | 30 | no | Seconds to wait before auto-filling |
| `minRealPlayers` | int | 1 | no | Real players needed before auto-fill triggers |

## Game Mode IDs

| ID | Display Name | Respawns | Key Fields |
|----|-------------|----------|------------|
| `duel` | Duel | no | — |
| `lms` | Last Man Standing | no | — |
| `deathmatch` | Deathmatch | yes | `killTarget`, `respawnDelaySeconds` |
| `koth` | King of the Hill | yes | `scoreTarget`, `captureZones`, `zoneRotationSeconds`, `respawnDelaySeconds` |

## Game-Mode-Specific Fields

### Deathmatch (`deathmatch`)

| Field | Type | Default | Required | Description |
|-------|------|---------|----------|-------------|
| `killTarget` | int | 0 | no | Kills needed to win. 0 = win by most kills at time limit |
| `respawnDelaySeconds` | int | 3 | no | Seconds before a dead player respawns |

### King of the Hill (`koth`)

| Field | Type | Default | Required | Description |
|-------|------|---------|----------|-------------|
| `scoreTarget` | int | 0 | no | Control-seconds to win. 0 = win by most control at time limit |
| `respawnDelaySeconds` | int | 3 | no | Seconds before a dead player respawns |
| `captureZones` | CaptureZone[] | null | yes (for koth) | One or more capture zones |
| `zoneRotationSeconds` | int | 60 | no | Seconds between zone rotations (ignored if only 1 zone) |

### Duel (`duel`) / Last Man Standing (`lms`)

No additional fields beyond core. These modes are elimination-based with no respawns.

## Object Schemas

### SpawnPoint

```json
{
  "x": 3.75,
  "y": 81.0,
  "z": -29.5,
  "yaw": 8.34,
  "pitch": 0.0
}
```

### Bounds

```json
{
  "minX": -11.0,
  "minY": 75.0,
  "minZ": -38.0,
  "maxX": 6.0,
  "maxY": 90.0,
  "maxZ": -21.0
}
```

### CaptureZone

```json
{
  "displayName": "Center Hill",
  "minX": 10.0,
  "minY": 60.0,
  "minZ": 10.0,
  "maxX": 20.0,
  "maxY": 65.0,
  "maxZ": 20.0
}
```

## Full Examples

### Duel

```json
{
  "id": "test_duel",
  "displayName": "Room42: Duel",
  "worldName": "default",
  "gameMode": "duel",
  "minPlayers": 2,
  "maxPlayers": 2,
  "waitTimeSeconds": 5,
  "botDifficulty": "MEDIUM",
  "botModelId": "Blook_Skeleton_Pirate_Gunner_Blunderbuss",
  "autoFillEnabled": true,
  "autoFillDelaySeconds": 30,
  "minRealPlayers": 1,
  "allowedKits": ["rogue", "warrior", "archer"],
  "spawnPoints": [
    { "x": 3.75, "y": 81.0, "z": -29.5, "yaw": 8.34, "pitch": 0.0 },
    { "x": -9.09, "y": 81.0, "z": -29.4, "yaw": 10.96, "pitch": 0.0 }
  ],
  "bounds": {
    "minX": -11.0, "minY": 75.0, "minZ": -38.0,
    "maxX": 6.0, "maxY": 90.0, "maxZ": -21.0
  }
}
```

### Deathmatch

```json
{
  "id": "ffa_arena",
  "displayName": "The Gauntlet: FFA",
  "worldName": "default",
  "gameMode": "deathmatch",
  "minPlayers": 2,
  "maxPlayers": 6,
  "waitTimeSeconds": 10,
  "matchDurationSeconds": 180,
  "killTarget": 15,
  "respawnDelaySeconds": 3,
  "allowedKits": ["warrior", "archer", "rogue"],
  "spawnPoints": [
    { "x": 0.0, "y": 80.0, "z": 0.0, "yaw": 0.0, "pitch": 0.0 },
    { "x": 10.0, "y": 80.0, "z": 0.0, "yaw": 180.0, "pitch": 0.0 },
    { "x": 0.0, "y": 80.0, "z": 10.0, "yaw": 90.0, "pitch": 0.0 },
    { "x": -10.0, "y": 80.0, "z": 0.0, "yaw": 270.0, "pitch": 0.0 },
    { "x": 5.0, "y": 80.0, "z": 5.0, "yaw": 135.0, "pitch": 0.0 },
    { "x": -5.0, "y": 80.0, "z": -5.0, "yaw": 315.0, "pitch": 0.0 }
  ],
  "bounds": {
    "minX": -15.0, "minY": 75.0, "minZ": -15.0,
    "maxX": 15.0, "maxY": 90.0, "maxZ": 15.0
  }
}
```

### King of the Hill (single zone)

```json
{
  "id": "hilltop",
  "displayName": "Hilltop: KOTH",
  "worldName": "default",
  "gameMode": "koth",
  "minPlayers": 2,
  "maxPlayers": 4,
  "waitTimeSeconds": 10,
  "matchDurationSeconds": 300,
  "scoreTarget": 120,
  "respawnDelaySeconds": 5,
  "allowedKits": ["warrior", "archer", "rogue"],
  "captureZones": [
    {
      "displayName": "The Summit",
      "minX": -3.0, "minY": 84.0, "minZ": -3.0,
      "maxX": 3.0, "maxY": 88.0, "maxZ": 3.0
    }
  ],
  "spawnPoints": [
    { "x": 10.0, "y": 80.0, "z": 0.0, "yaw": 270.0, "pitch": 0.0 },
    { "x": -10.0, "y": 80.0, "z": 0.0, "yaw": 90.0, "pitch": 0.0 },
    { "x": 0.0, "y": 80.0, "z": 10.0, "yaw": 0.0, "pitch": 0.0 },
    { "x": 0.0, "y": 80.0, "z": -10.0, "yaw": 180.0, "pitch": 0.0 }
  ],
  "bounds": {
    "minX": -20.0, "minY": 75.0, "minZ": -20.0,
    "maxX": 20.0, "maxY": 95.0, "maxZ": 20.0
  }
}
```

### King of the Hill (rotating zones)

```json
{
  "id": "koth_rotator",
  "displayName": "Fortress: KOTH",
  "worldName": "world2",
  "gameMode": "koth",
  "minPlayers": 2,
  "maxPlayers": 6,
  "waitTimeSeconds": 10,
  "matchDurationSeconds": 300,
  "scoreTarget": 90,
  "respawnDelaySeconds": 3,
  "zoneRotationSeconds": 45,
  "captureZones": [
    {
      "displayName": "East Tower",
      "minX": 15.0, "minY": 85.0, "minZ": -3.0,
      "maxX": 21.0, "maxY": 89.0, "maxZ": 3.0
    },
    {
      "displayName": "West Tower",
      "minX": -21.0, "minY": 85.0, "minZ": -3.0,
      "maxX": -15.0, "maxY": 89.0, "maxZ": 3.0
    },
    {
      "displayName": "Center Bridge",
      "minX": -4.0, "minY": 82.0, "minZ": -2.0,
      "maxX": 4.0, "maxY": 86.0, "maxZ": 2.0
    }
  ],
  "spawnPoints": [
    { "x": 20.0, "y": 80.0, "z": 10.0, "yaw": 225.0, "pitch": 0.0 },
    { "x": -20.0, "y": 80.0, "z": 10.0, "yaw": 315.0, "pitch": 0.0 },
    { "x": 20.0, "y": 80.0, "z": -10.0, "yaw": 135.0, "pitch": 0.0 },
    { "x": -20.0, "y": 80.0, "z": -10.0, "yaw": 45.0, "pitch": 0.0 },
    { "x": 0.0, "y": 80.0, "z": 12.0, "yaw": 0.0, "pitch": 0.0 },
    { "x": 0.0, "y": 80.0, "z": -12.0, "yaw": 180.0, "pitch": 0.0 }
  ],
  "bounds": {
    "minX": -25.0, "minY": 75.0, "minZ": -15.0,
    "maxX": 25.0, "maxY": 95.0, "maxZ": 15.0
  }
}
```
