# NPC API Patterns & Techniques

> Extracted from working Hytale server code (HycompanionAdapter)

---

## Table of Contents

1. [Spawning NPCs](#1-spawning-npcs)
2. [Removing NPCs](#2-removing-npcs)
3. [Invulnerability](#3-invulnerability)
4. [Knockback Control](#4-knockback-control)
5. [Animation Control](#5-animation-control)
6. [State Machine Access](#6-state-machine-access)
7. [Combat Support](#7-combat-support)
8. [Inventory & Weapon Equipping](#8-inventory--weapon-equipping)
9. [Entity Discovery (World Scanning)](#9-entity-discovery-world-scanning)
10. [Nameplates & Floating Text](#10-nameplates--floating-text)
11. [Model & Animation Discovery](#11-model--animation-discovery)
12. [Spawning Lightweight Entities](#12-spawning-lightweight-entities-holograms-markers)
13. [Player Teleportation](#13-player-teleportation)
14. [Utility: World from Entity Store](#14-utility-world-from-entity-store)
15. [Utility: Role Name Formatting](#15-utility-role-name-formatting)

---

## 1. Spawning NPCs

Two spawn methods exist:

### By role index (preferred by Hycompanion)

```java
NPCPlugin npcPlugin = NPCPlugin.get();
int roleIndex = npcPlugin.getIndex(externalId); // returns -1 if not found

if (roleIndex >= 0) {
    var npcPair = npcPlugin.spawnEntity(
        store,        // Store<EntityStore>
        roleIndex,    // int — role index
        position,     // Vector3d
        rotation,     // Vector3f
        null,         // spawnModel — null = use default from role
        null           // postSpawn callback
    );
    // Returns: Pair<Ref<EntityStore>, NPCEntity>
}
```

### By model name string

```java
var result = npcPlugin.spawnNPC(store, "Blook_Skeleton_Pirate_Captain", null, position, rotation);
// Returns: Pair<Ref<EntityStore>, INonPlayerCharacter>
```

### Critical post-spawn setup

WanderInCircle uses the leash point as its center. Without this, NPCs wander from world origin:

```java
npcEntity.setLeashPoint(position);
npcEntity.setLeashHeading(rotation.getYaw());
npcEntity.setLeashPitch(rotation.getPitch());
```

---

## 2. Removing NPCs

### Clean NPC removal

```java
NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
if (npcEntity != null) {
    npcEntity.remove(); // Proper NPC removal with death/despawn handling
}
```

### Generic entity removal (for markers, holograms)

```java
store.removeEntity(ref, RemoveReason.REMOVE);
```

### Always check validity first

```java
if (entityRef.isValid()) {
    Store<EntityStore> store = entityRef.getStore();
    // ... safe to operate
}
```

---

## 3. Invulnerability

Toggle damage immunity via `EffectControllerComponent`:

```java
EffectControllerComponent effect = store.getComponent(
    entityRef, EffectControllerComponent.getComponentType());

if (effect != null) {
    effect.setInvulnerable(true);  // Immune to all damage
    effect.setInvulnerable(false); // Normal damage
}
```

**Import:** `com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent`

---

## 4. Knockback Control

Three levels of knockback control — all three are needed for it to stick:

### Active motion controller

```java
MotionController activeController = role.getActiveMotionController();
if (activeController != null) {
    activeController.setKnockbackScale(0.0); // 0 = no knockback, 1 = normal
}
```

### All motion controllers (reflection)

The role can switch controllers, so update them all:

```java
Field controllersField = Role.class.getDeclaredField("motionControllers");
controllersField.setAccessible(true);

@SuppressWarnings("unchecked")
Map<String, MotionController> controllers =
    (Map<String, MotionController>) controllersField.get(role);

if (controllers != null) {
    for (MotionController mc : controllers.values()) {
        mc.setKnockbackScale(0.0);
    }
}
```

### Role master field (reflection) — prevents periodic resets

`Role.updateMotionControllers()` periodically resets all controllers to the master value:

```java
Field scaleField = Role.class.getDeclaredField("knockbackScale");
scaleField.setAccessible(true);
scaleField.setDouble(role, 0.0);
```

**Import:** `com.hypixel.hytale.server.npc.movement.controllers.MotionController`

---

## 5. Animation Control

### Play an animation

```java
npcEntity.playAnimation(entityRef, AnimationSlot.Action, "Sit", store);
```

Animation names come from the model's AnimationSets (e.g., `Idle`, `Walk`, `Run`, `Sit`, `Sleep`, `Howl`, `Greet`).

### Stop an animation

```java
AnimationUtils.stopAnimation(entityRef, AnimationSlot.Action, true, store);
```

### Reset to idle (recommended before state changes)

```java
AnimationUtils.stopAnimation(entityRef, AnimationSlot.Action, true, store);
npcEntity.playAnimation(entityRef, AnimationSlot.Action, "Idle", store);
```

**Imports:**
```java
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
```

---

## 6. State Machine Access

### Get current state

```java
String currentState = role.getStateSupport().getStateName();
```

### Set state

```java
role.getStateSupport().setState(entityRef, "Follow", "Default", store);
role.getStateSupport().setState(entityRef, "Combat", "Attack", store);
role.getStateSupport().setState(entityRef, "Idle", "Default", store);
```

### Check if a state exists before setting it

```java
StateMappingHelper stateHelper = role.getStateSupport().getStateHelper();

// Check state
if (stateHelper.getStateIndex("Combat") >= 0) {
    // State exists — safe to set
}

// Check sub-state
int stateIdx = stateHelper.getStateIndex("Chase");
if (stateIdx >= 0 && stateHelper.getSubStateIndex(stateIdx, "Attack") >= 0) {
    // Chase.Attack sub-state exists
}
```

### Check if NPC is busy

```java
if (role.getStateSupport().isInBusyState()) {
    // Don't interrupt — NPC is in combat, following, etc.
}
```

**Import:** `com.hypixel.hytale.server.npc.asset.builder.StateMappingHelper`

---

## 7. Combat Support

### Override attack type

```java
CombatSupport combatSupport = role.getCombatSupport();
if (combatSupport != null) {
    combatSupport.clearAttackOverrides();
    combatSupport.addAttackOverride("Attack=Melee");
    // or: combatSupport.addAttackOverride("Attack=Ranged");
}
```

### Clear overrides when stopping combat

```java
combatSupport.clearAttackOverrides();
```

**Import:** `com.hypixel.hytale.server.npc.role.support.CombatSupport`

---

## 8. Inventory & Weapon Equipping

### Access NPC inventory

```java
Inventory inventory = npcEntity.getInventory();
ItemContainer hotbar = inventory.getHotbar();
```

### Check current weapon

```java
int currentSlot = inventory.getActiveHotbarSlot();
ItemStack currentItem = hotbar.getItemStack((short) currentSlot);
boolean hasWeapon = (currentItem != null && !currentItem.isEmpty());
```

### Auto-equip first available weapon

```java
for (short i = 0; i < hotbar.getCapacity(); i++) {
    ItemStack item = hotbar.getItemStack(i);
    if (item != null && !item.isEmpty()) {
        inventory.setActiveHotbarSlot((byte) i);
        break;
    }
}
```

**Imports:**
```java
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
```

---

## 9. Entity Discovery (World Scanning)

Iterate all NPCs in all worlds to find entities by role:

```java
int targetRoleIndex = NPCPlugin.get().getIndex("MyRoleName");

for (World world : Universe.get().getWorlds().values()) {
    world.execute(() -> {
        Store<EntityStore> store = world.getEntityStore().getStore();

        store.forEachChunk(NPCEntity.getComponentType(), (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());

                if (npc != null && npc.getRoleIndex() == targetRoleIndex) {
                    Ref<EntityStore> entityRef = chunk.getReferenceTo(i);
                    UUID entityUuid = npc.getUuid();

                    // Found a matching NPC — store reference
                }
            }
        });
    });
}
```

**Import:** `com.hypixel.hytale.component.ArchetypeChunk`

---

## 10. Nameplates & Floating Text

### Add nameplate to an entity

```java
Nameplate nameplate = new Nameplate("Hello World");
holder.addComponent(Nameplate.getComponentType(), nameplate);
```

### Update nameplate text

```java
Nameplate nameplate = store.getComponent(entityRef, Nameplate.getComponentType());
if (nameplate != null) {
    nameplate.setText("New text");
}
```

### Read nameplate from existing entity

```java
Nameplate np = store.getComponent(entityRef, Nameplate.getComponentType());
String text = np != null ? np.getText() : null;
```

**Import:** `com.hypixel.hytale.server.core.entity.nameplate.Nameplate`

---

## 11. Model & Animation Discovery

### Get model from entity

```java
ModelComponent modelComponent = store.getComponent(entityRef, ModelComponent.getComponentType());
Model model = modelComponent.getModel();
```

### List all available animations

```java
Set<String> animationNames = model.getAnimationSetMap().keySet();
// e.g., ["Idle", "Walk", "Run", "Sit", "Sleep", "Attack", "Greet", ...]
```

### Get role info

```java
String roleName = npcEntity.getRoleName();      // e.g., "Blook_Skeleton_Pirate_Captain"
int roleIndex = npcEntity.getRoleIndex();        // numeric index
Role role = npcEntity.getRole();                 // Role object
boolean needsLeash = role.requiresLeashPosition();
```

**Imports:**
```java
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
```

---

## 12. Spawning Lightweight Entities (Holograms, Markers)

For invisible target entities, floating text, or waypoints:

```java
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

// Lightweight base entity
ProjectileComponent projectile = new ProjectileComponent("Projectile");
holder.putComponent(ProjectileComponent.getComponentType(), projectile);

// Position
holder.putComponent(TransformComponent.getComponentType(),
    new TransformComponent(position, new Vector3f(0, 0, 0)));

// Required basics
holder.ensureComponent(UUIDComponent.getComponentType());
holder.ensureComponent(Intangible.getComponentType()); // No collision

// NetworkId — required for client visibility and spatial registration
holder.addComponent(NetworkId.getComponentType(),
    new NetworkId(store.getExternalData().takeNextNetworkId()));

// Optional: floating text
holder.addComponent(Nameplate.getComponentType(), new Nameplate("Label"));

projectile.initialize();

// Spawn
Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);

// Later: remove
store.removeEntity(ref, RemoveReason.REMOVE);
```

**Imports:**
```java
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
```

---

## 13. Player Teleportation

Uses the `Teleport` component — adding it to the entity triggers the teleport:

```java
Ref<EntityStore> playerRef = targetPlayer.getReference();
Store<EntityStore> store = playerRef.getStore();

// Get current rotation to preserve it
TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
HeadRotation headRotation = store.getComponent(playerRef, HeadRotation.getComponentType());

Vector3f bodyRot = transform.getRotation();
Vector3f headRot = headRotation.getRotation();

// Create teleport
Vector3d newPos = new Vector3d(x, y, z);
Teleport teleport = new Teleport(newPos, bodyRot);
teleport.setHeadRotation(headRot);

// Adding the component triggers the teleport
store.addComponent(playerRef, Teleport.getComponentType(), teleport);
```

**Imports:**
```java
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
```

---

## 14. Utility: World from Entity Store

Get the World an entity belongs to without knowing it upfront:

```java
Store<EntityStore> store = entityRef.getStore();
World world = store.getExternalData().getWorld();
```

Get next network ID for spawning:

```java
int networkId = store.getExternalData().takeNextNetworkId();
```

---

## 15. Utility: Role Name Formatting

Convert role IDs to display names:

```java
// "hytale:Blook_Skeleton_Pirate_Captain" → "Blook Skeleton Pirate Captain"
String name = roleName;

// Strip namespace
int colonIndex = name.indexOf(':');
if (colonIndex >= 0) name = name.substring(colonIndex + 1);

// Capitalize words
String[] words = name.split("_");
StringBuilder result = new StringBuilder();
for (int i = 0; i < words.length; i++) {
    if (i > 0) result.append(" ");
    result.append(Character.toUpperCase(words[i].charAt(0)));
    result.append(words[i].substring(1).toLowerCase());
}
```

---

## Quick Reference: Role Supports

Access specialized APIs through `Role`:

```java
Role role = npcEntity.getRole();

role.getStateSupport()          // State machine (get/set state, check busy)
role.getMarkedEntitySupport()   // Target management (LockedTarget, etc.)
role.getCombatSupport()         // Combat overrides (melee/ranged)
role.getActiveMotionController() // Current movement controller
role.requiresLeashPosition()    // Whether NPC needs a leash point
```
