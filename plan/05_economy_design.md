# Phase 7: Economy & Shop Design

## Overview

Two independent progression systems, both local-first with API sync planned later.

1. **ArenaPoints (AP)** — spendable currency for shop purchases
2. **Honor** — activity-based reputation metric that grants rank permissions

Both are permission-driven. The economy system manages permissions — it doesn't need to know about kits or other systems directly.

---

## Honor System

### Purpose
Measures how active and familiar a player is with HyArena. Encourages regular play through slow decay — you must keep playing to maintain your rank.

### Mechanics
- **Gained** by playing matches
- **Decays** over time (server-processed, continuous, even while offline)
- **Capped** at 1200
- **Rank thresholds** determine permission group assignments

### Numbers

| Parameter | Value |
|-----------|-------|
| Honor per match | ~10 (TBD: wins vs losses) |
| Decay rate | ~25/day (~1/hour), continuous |
| Global cap | 1200 |
| Design target | 10 games/day = ~100 honor gained, decays over 4 days (1:4 ratio) |

**Steady state at 10 games/day:** +75 net honor/day. Reach max rank in ~13-14 days.
**Full decay from cap (1200):** ~48 days to reach 0 if completely inactive.

### Honor Ranks

| Threshold | Rank | Permission |
|-----------|------|------------|
| 0 | Novice | `hyarena.classes.rank.novice` |
| 100 | TBD | `hyarena.classes.rank.<name>` |
| 300 | TBD | `hyarena.classes.rank.<name>` |
| 600 | TBD | `hyarena.classes.rank.<name>` |
| 1000 | TBD | `hyarena.classes.rank.<name>` |

- The 200-point buffer above rank 5 (1000–1200) lets players miss a few days without losing their highest rank.
- Rank names are TBD — will be configurable.
- Eventually, arenas may also be gated by honor rank (not for now).

### Rank Permissions
When a player's honor crosses a threshold, they are assigned to the corresponding permission group. When it drops below, the permission is removed. The system checks on each honor update (gain or decay tick).

---

## ArenaPoints (AP)

### Purpose
Spendable currency earned from playing matches. Used to purchase items in the shop.

### Earn Rates

| Outcome | ArenaPoints |
|---------|-------------|
| Win | ~15 |
| Loss | ~10 |

- Not punishing for losses — still rewards participation.
- Can also be purchased for real money via website (later, API sync phase).

---

## Kit Unlock Paths

Three independent, permission-based unlock paths:

| Path | Permission Pattern | How Unlocked |
|------|-------------------|--------------|
| Honor rank kits | `hyarena.classes.rank.<rank>` | Automatically when honor reaches threshold |
| Shop kits | `hyarena.classes.purchased.<kitId>` | Bought with ArenaPoints |
| Premium kits | `hyarena.classes.premium` | One-time premium membership purchase |

All three are separate — no overlap required. A kit's config specifies which permission it requires, and the existing `KitAccessInfo` system handles the rest.

---

## Shop

### Structure
Shop items defined in config, organized by categories:

- **Service** — Premium membership (and future service-type purchases)
- **Kits** — Kits purchasable with ArenaPoints

### Config Format
Config file(s) in `config/shop/` with items grouped by category. Each item has:
- ID
- Category
- Display name / description
- Cost (in ArenaPoints)
- Permission granted on purchase

### Purchase Flow
1. Player opens shop UI, browses categories
2. Selects an item → sees cost, description
3. Confirms purchase → AP deducted, permission granted, transaction logged
4. Item shows as "owned" in shop going forward

### Premium Membership
- Purchasable in-game via the shop (Service category)
- On purchase, player is assigned to the `Premium` permission group
- Grants access to all kits requiring `hyarena.classes.premium`
- One-time purchase (not a subscription)
- Price in ArenaPoints: TBD

---

## Storage (Local-First)

### Per-Player Data
JSON file per player, stored locally. Contains:
- Current honor value
- Current ArenaPoints balance
- Last online timestamp (for decay calculation)
- List of purchased item IDs / granted permissions

### Transaction Log
Per-player log recording:
- Purchases (item, cost, timestamp)
- AP gains (amount, reason/match ID, timestamp)
- Honor changes (amount, reason, timestamp)

### File Location
`data/players/<uuid>.json` — player economy data
`data/players/<uuid>_transactions.json` — transaction log

### Migration Path
When API sync is added (Phase 10+), the local store becomes a cache/fallback, and the API becomes the source of truth. The transaction log format should be designed to support batch-upload to the API.

---

## Architecture

### New Classes
- `HonorManager` — tracks honor per player, processes decay, assigns/removes rank permissions
- `EconomyManager` (or `ArenaPointsManager`) — tracks AP balance, handles earn/spend
- `ShopManager` — loads shop config, processes purchases (deduct AP + grant permission)
- `PlayerEconomyData` — data class for per-player state
- `TransactionLog` — append-only log writer

### Events
- `CurrencyEarnedEvent(Player, int amount, String reason)`
- `PurchaseEvent(Player, String itemId, int cost)`
- `HonorChangedEvent(Player, double oldHonor, double newHonor)`
- `HonorRankChangedEvent(Player, String oldRank, String newRank)`

### Integration Points
- `Match.java` — on match end, award AP and honor to participants
- `HyArena2.java` — scheduler tick for honor decay processing
- `KitManager` / `KitAccessInfo` — already permission-based, no changes needed

---

## Shop UI (TBD)

**Entry point:** `OpenShopInteraction.java` — already wired to a hub statue, currently shows placeholder message. Will open the shop page once built.

Shop interface design to be discussed at end of Phase 7 implementation. Topics:
- Page layout and navigation between categories
- Item display (name, description, cost, owned status)
- Purchase confirmation flow
- Premium membership presentation
- Currency display (AP balance, current honor/rank)

---

## Open Questions

- [ ] Exact honor gain for wins vs losses (same? slightly more for wins?)
- [ ] Honor rank names (novice + 4 more)
- [ ] Premium membership price in ArenaPoints
- [ ] Specific AP/honor amounts for different game modes (e.g., longer KOTH matches worth more?)
- [ ] Should transaction log have a max size / rotation?
