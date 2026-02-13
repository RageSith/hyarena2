# Web API Guide — For Plugin Integration (Phase 10)

## Overview

The website is a **Slim 4 PHP** app with Twig templates, PDO/MySQL, and pure ES6 frontend. No ORM — raw SQL with prepared statements. The plugin communicates with the website via **3 POST endpoints** protected by API key.

## Plugin → Website Endpoints

All require header `X-API-Key: <configured key>` (or query param `api_key`). API key is validated with `hash_equals` (constant-time).

---

### 1. POST `/api/match/submit` — Submit a completed match

**Called after**: match ends (FINISHED state)

**Request body (JSON):**
```json
{
  "arena_id": "test_duel",
  "game_mode": "duel",
  "winner_uuid": "uuid-string-or-null",
  "duration_seconds": 124,
  "started_at": "2026-02-13T14:30:00Z",
  "ended_at": "2026-02-13T14:32:04Z",
  "participants": [
    {
      "uuid": "player-uuid",
      "username": "PlayerName",
      "is_bot": false,
      "kit_id": "warrior",
      "pvp_kills": 3,
      "pvp_deaths": 1,
      "pve_kills": 0,
      "pve_deaths": 0,
      "damage_dealt": 245.5,
      "damage_taken": 102.3,
      "is_winner": true
    },
    {
      "uuid": null,
      "username": "Bot_Steve",
      "is_bot": true,
      "bot_difficulty": "MEDIUM",
      "kit_id": "archer",
      "pvp_kills": 1,
      "pvp_deaths": 3,
      "pve_kills": 0,
      "pve_deaths": 0,
      "damage_dealt": 102.3,
      "damage_taken": 245.5,
      "is_winner": false
    }
  ]
}
```

**What happens server-side (MatchSubmissionService in transaction):**
1. Upsert each real player into `players` table (uuid, username, first_seen, last_seen)
2. Create `matches` record
3. Create `match_participants` records (both players and bots)
4. Update `player_stats` per-arena row for each real player (matches_played, wins/losses, kills, deaths, damage, time)
5. Update `player_stats` global row (arena_id=NULL) for each real player
6. Bots are recorded in match_participants but do NOT update player_stats

**Response:**
```json
{ "success": true, "match_id": 42 }
```

---

### 2. POST `/api/sync` — Sync arenas and kits

**Called on**: plugin startup, or when arena/kit configs change

**Request body (JSON):**
```json
{
  "arenas": [
    {
      "id": "test_duel",
      "display_name": "The Pit",
      "description": "1v1 arena in the pit",
      "game_mode": "duel",
      "world_name": "default",
      "min_players": 2,
      "max_players": 2,
      "icon": null
    }
  ],
  "kits": [
    {
      "id": "warrior",
      "display_name": "Warrior",
      "description": "Heavy armor, devastating melee",
      "icon": null
    }
  ]
}
```

**What happens:** Upserts arenas and kits using `ON DUPLICATE KEY UPDATE`.

**Response:**
```json
{ "success": true, "synced": { "arenas": 3, "kits": 4 } }
```

---

### 3. POST `/api/link/generate` — Generate account link code

**Called when**: player runs `/link` in-game

**Request body (JSON):**
```json
{
  "player_uuid": "uuid-string",
  "username": "PlayerName"
}
```

**What happens:**
1. Upserts player into `players` table
2. Generates 6-char code (charset: `ABCDEFGHJKMNPQRSTUVWXYZ23456789` — no ambiguous 0/O/1/I/L)
3. Stores in `link_codes` table with 10-minute expiry

**Response:**
```json
{ "success": true, "code": "HK7M3N", "expires_in": 600 }
```

---

## Public API Endpoints (read by frontend JS)

These are GET, rate-limited (60 req/min per IP), no auth required.

### GET `/api/server-stats`
Returns aggregate stats. **The plugin doesn't call this** — the website calculates from DB.
```json
{
  "success": true,
  "data": {
    "total_matches": 1234,
    "total_players": 567,
    "total_kills": 8901,
    "matches_today": 42,
    "online_players": 15,
    "max_players": 100
  }
}
```
Note: `online_players`/`max_players` come from a separate server status query (currently hardcoded or from the game server's query protocol).

### GET `/api/leaderboard?arena=global&sort=pvp_kills&order=DESC&page=1&per_page=25&limit=5`
- `arena`: arena_id or "global" (default: global)
- `sort`: pvp_kills, matches_won, pvp_kd_ratio, win_rate, pve_kills (default: pvp_kills)
- `limit`: shorthand for per_page (used by home page for top 5)
- Returns entries with: rank, username, player_uuid, matches_played, matches_won, pvp_kills, pvp_deaths, pvp_kd_ratio, win_rate, pve_kills, pve_deaths

Frontend home page fetches `?limit=5` for "Top Fighters" widget. Leaderboard page uses full pagination.

### GET `/api/recent-matches?limit=5`
Returns recent matches with winner info, participants, duration, arena name.
Frontend renders as clickable entries that open a match detail modal.

### GET `/api/arenas`
Returns all arenas. Frontend renders as a table (not cards) with columns: Arena, Players, Match Length, Mode, Respawn, Spawns, Size.
Response: `{ "success": true, "data": { "arenas": [...] } }`
Each arena includes: id, display_name, description, game_mode, min_players, max_players, match_duration_seconds, win_condition, kill_target, allow_respawn, spawn_point_count, area_size_blocks.

### GET `/api/kits`
Returns all kits. Not currently used by home page (kits are hardcoded HTML). Available for future use.

### GET `/api/player/{name_or_uuid}`
Returns full player profile: player info, global stats, per-arena stats, recent matches.
Used by `/player/{name}` page. Links from leaderboard entries and match participants.

---

## Database Schema (key tables)

**players**: uuid (PK), username (unique), first_seen, last_seen, arena_points, honor, honor_rank, currency

**matches**: id (PK, auto), arena_id (FK), game_mode, winner_uuid (FK, nullable), duration_seconds, started_at, ended_at

**match_participants**: id (PK, auto), match_id (FK), player_uuid (FK, nullable for bots), is_bot, bot_name, bot_difficulty, kit_id, pvp_kills, pvp_deaths, pve_kills, pve_deaths, damage_dealt, damage_taken, is_winner

**player_stats**: id (PK, auto), player_uuid (FK), arena_id (nullable, NULL=global), matches_played, matches_won, matches_lost, pvp_kills, pvp_deaths, pve_kills, pve_deaths, damage_dealt, damage_taken, total_time_played
- Generated columns: pvp_kd_ratio, win_rate (computed by MySQL)
- Unique index on (player_uuid, arena_id)

**link_codes**: code (6-char), player_uuid, expires_at, used

---

## What the Plugin Needs to Implement (Phase 10)

1. **HTTP client** — POST JSON to website endpoints with API key header
2. **Match submission** — after match FINISHED, serialize match data and call `/api/match/submit`
3. **Config sync** — on plugin startup (or reload), call `/api/sync` with all arena configs + kit configs
4. **Link command** — `/link` command calls `/api/link/generate`, shows code to player in chat/UI
5. **Config** — API base URL and API key in global.json or dedicated api.json
6. **Async** — HTTP calls should be async (not on world thread). Use CompletableFuture or scheduler

### Key data the plugin tracks that maps to submission:
- `Participant.getKills()` → pvp_kills (need to split pvp vs pve if bot kills tracked separately)
- `Participant.getDeaths()` → pvp_deaths
- `Participant.getDamageDealt()` → damage_dealt
- `Participant.getDamageReceived()` → damage_taken (called damage_received in some places)
- `Match.getWinner()` → winner_uuid
- `Match.getDurationSeconds()` → duration_seconds
- `ArenaConfig` fields → arena sync payload
- `KitConfig` fields → kit sync payload

### PvP vs PvE kill separation:
The database separates pvp_kills/pvp_deaths from pve_kills/pve_deaths. The plugin's KillDetectionSystem needs to track whether the killer/victim was a bot or player to populate these correctly.
