# Hytale Server API Import Reference

This document maps class names to their full import paths for the Hytale Server API.

---

## Commands

### Base Classes
| Class | Import |
|-------|--------|
| `AbstractCommand` | `com.hypixel.hytale.server.core.command.system.AbstractCommand` |
| `AbstractPlayerCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand` |
| `AbstractWorldCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand` |
| `AbstractAsyncCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand` |
| `AbstractAsyncPlayerCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand` |
| `AbstractAsyncWorldCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncWorldCommand` |
| `AbstractTargetPlayerCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand` |
| `AbstractTargetEntityCommand` | `com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand` |
| `CommandBase` | `com.hypixel.hytale.server.core.command.system.basecommands.CommandBase` |

### Command System
| Class | Import |
|-------|--------|
| `CommandContext` | `com.hypixel.hytale.server.core.command.system.CommandContext` |
| `CommandManager` | `com.hypixel.hytale.server.core.command.system.CommandManager` |
| `CommandRegistry` | `com.hypixel.hytale.server.core.command.system.CommandRegistry` |
| `CommandRegistration` | `com.hypixel.hytale.server.core.command.system.CommandRegistration` |
| `CommandSender` | `com.hypixel.hytale.server.core.command.system.CommandSender` |
| `CommandUtil` | `com.hypixel.hytale.server.core.command.system.CommandUtil` |
| `CommandOwner` | `com.hypixel.hytale.server.core.command.system.CommandOwner` |

### Command Exceptions
| Class | Import |
|-------|--------|
| `CommandException` | `com.hypixel.hytale.server.core.command.system.exceptions.CommandException` |
| `GeneralCommandException` | `com.hypixel.hytale.server.core.command.system.exceptions.GeneralCommandException` |
| `NoPermissionException` | `com.hypixel.hytale.server.core.command.system.exceptions.NoPermissionException` |
| `SenderTypeException` | `com.hypixel.hytale.server.core.command.system.exceptions.SenderTypeException` |

---

## Players & Entities

### Core Entity Classes
| Class | Import |
|-------|--------|
| `Entity` | `com.hypixel.hytale.server.core.entity.Entity` |
| `LivingEntity` | `com.hypixel.hytale.server.core.entity.LivingEntity` |
| `Player` | `com.hypixel.hytale.server.core.entity.entities.Player` |
| `BlockEntity` | `com.hypixel.hytale.server.core.entity.entities.BlockEntity` |
| `PlayerRef` | `com.hypixel.hytale.server.core.universe.PlayerRef` |

### Entity Utilities
| Class | Import |
|-------|--------|
| `EntityUtils` | `com.hypixel.hytale.server.core.entity.EntityUtils` |
| `EntitySnapshot` | `com.hypixel.hytale.server.core.entity.EntitySnapshot` |
| `AnimationUtils` | `com.hypixel.hytale.server.core.entity.AnimationUtils` |
| `ItemUtils` | `com.hypixel.hytale.server.core.entity.ItemUtils` |

### Player Input & Movement
| Class | Import |
|-------|--------|
| `PlayerInput` | `com.hypixel.hytale.server.core.modules.entity.player.PlayerInput` |
| `MovementStatesComponent` | `com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent` |

### Entity Effects & Damage
| Class | Import |
|-------|--------|
| `EffectControllerComponent` | `com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent` |
| `ActiveEntityEffect` | `com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect` |
| `KnockbackComponent` | `com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent` |
| `KnockbackSystems` | `com.hypixel.hytale.server.core.entity.knockback.KnockbackSystems` |
| `DamageDataComponent` | `com.hypixel.hytale.server.core.entity.damage.DamageDataComponent` |

---

## World & Universe

### World Management
| Class | Import |
|-------|--------|
| `World` | `com.hypixel.hytale.server.core.universe.world.World` |
| `Universe` | `com.hypixel.hytale.server.core.universe.Universe` |
| `WorldConfig` | `com.hypixel.hytale.server.core.universe.world.WorldConfig` |
| `WorldConfigProvider` | `com.hypixel.hytale.server.core.universe.world.WorldConfigProvider` |
| `WorldProvider` | `com.hypixel.hytale.server.core.universe.world.WorldProvider` |

### World Utilities
| Class | Import |
|-------|--------|
| `IWorldChunks` | `com.hypixel.hytale.server.core.universe.world.IWorldChunks` |
| `IWorldChunksAsync` | `com.hypixel.hytale.server.core.universe.world.IWorldChunksAsync` |
| `PlaceBlockSettings` | `com.hypixel.hytale.server.core.universe.world.PlaceBlockSettings` |
| `SetBlockSettings` | `com.hypixel.hytale.server.core.universe.world.SetBlockSettings` |
| `ParticleUtil` | `com.hypixel.hytale.server.core.universe.world.ParticleUtil` |
| `SoundUtil` | `com.hypixel.hytale.server.core.universe.world.SoundUtil` |
| `SpawnUtil` | `com.hypixel.hytale.server.core.universe.world.SpawnUtil` |
| `PlayerUtil` | `com.hypixel.hytale.server.core.universe.world.PlayerUtil` |

### Data Storage
| Class | Import |
|-------|--------|
| `EntityStore` | `com.hypixel.hytale.server.core.universe.world.storage.EntityStore` |
| `DataStore` | `com.hypixel.hytale.server.core.universe.datastore.DataStore` |
| `DataStoreProvider` | `com.hypixel.hytale.server.core.universe.datastore.DataStoreProvider` |
| `DiskDataStore` | `com.hypixel.hytale.server.core.universe.datastore.DiskDataStore` |
| `PlayerStorage` | `com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage` |
| `PlayerStorageProvider` | `com.hypixel.hytale.server.core.universe.playerdata.PlayerStorageProvider` |

---

## UI & Pages

### UI Builders
| Class | Import |
|-------|--------|
| `UICommandBuilder` | `com.hypixel.hytale.server.core.ui.builder.UICommandBuilder` |
| `UIEventBuilder` | `com.hypixel.hytale.server.core.ui.builder.UIEventBuilder` |
| `EventData` | `com.hypixel.hytale.server.core.ui.builder.EventData` |

### UI Components
| Class | Import | Usage |
|-------|--------|-------|
| `Area` | `com.hypixel.hytale.server.core.ui.Area` | |
| `Anchor` | `com.hypixel.hytale.server.core.ui.Anchor` | |
| `Value` | `com.hypixel.hytale.server.core.ui.Value` | |
| `LocalizableString` | `com.hypixel.hytale.server.core.ui.LocalizableString` | DropdownBox display text |
| `PatchStyle` | `com.hypixel.hytale.server.core.ui.PatchStyle` | |
| `ItemGridSlot` | `com.hypixel.hytale.server.core.ui.ItemGridSlot` | |
| `DropdownEntryInfo` | `com.hypixel.hytale.server.core.ui.DropdownEntryInfo` | DropdownBox entries |

### Pages
| Class | Import |
|-------|--------|
| `BasicCustomUIPage` | `com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage` |
| `InteractiveCustomUIPage` | `com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage` |
| `CustomPageLifetime` | `com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime` |
| `CustomUIEventBindingType` | `com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType` |

### File Browser
| Class | Import |
|-------|--------|
| `ServerFileBrowser` | `com.hypixel.hytale.server.core.ui.browser.ServerFileBrowser` |
| `FileBrowserConfig` | `com.hypixel.hytale.server.core.ui.browser.FileBrowserConfig` |
| `FileBrowserEventData` | `com.hypixel.hytale.server.core.ui.browser.FileBrowserEventData` |

---

## Plugins

### Plugin Base Classes
| Class | Import |
|-------|--------|
| `JavaPlugin` | `com.hypixel.hytale.server.core.plugin.JavaPlugin` |
| `PluginBase` | `com.hypixel.hytale.server.core.plugin.PluginBase` |
| `JavaPluginInit` | `com.hypixel.hytale.server.core.plugin.JavaPluginInit` |
| `PluginInit` | `com.hypixel.hytale.server.core.plugin.PluginInit` |

### Plugin Management
| Class | Import |
|-------|--------|
| `PluginManager` | `com.hypixel.hytale.server.core.plugin.PluginManager` |
| `PluginClassLoader` | `com.hypixel.hytale.server.core.plugin.PluginClassLoader` |
| `PluginState` | `com.hypixel.hytale.server.core.plugin.PluginState` |
| `PluginType` | `com.hypixel.hytale.server.core.plugin.PluginType` |

### Plugin Registry
| Class | Import |
|-------|--------|
| `IRegistry` | `com.hypixel.hytale.server.core.plugin.registry.IRegistry` |
| `AssetRegistry` | `com.hypixel.hytale.server.core.plugin.registry.AssetRegistry` |
| `CodecMapRegistry` | `com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry` |
| `MapKeyMapRegistry` | `com.hypixel.hytale.server.core.plugin.registry.MapKeyMapRegistry` |

---

## Events

### Core Events
| Class | Import |
|-------|--------|
| `BootEvent` | `com.hypixel.hytale.server.core.event.events.BootEvent` |
| `PrepareUniverseEvent` | `com.hypixel.hytale.server.core.event.events.PrepareUniverseEvent` |
| `ShutdownEvent` | `com.hypixel.hytale.server.core.event.events.ShutdownEvent` |

### Player Events
| Class | Import |
|-------|--------|
| `PlayerEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerEvent` |
| `PlayerConnectEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent` |
| `PlayerDisconnectEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent` |
| `PlayerSetupConnectEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent` |
| `PlayerSetupDisconnectEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerSetupDisconnectEvent` |
| `PlayerReadyEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent` |
| `PlayerChatEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent` |
| `PlayerInteractEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent` |
| `PlayerCraftEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerCraftEvent` |
| `PlayerMouseButtonEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent` |
| `PlayerMouseMotionEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent` |
| `PlayerRefEvent` | `com.hypixel.hytale.server.core.event.events.player.PlayerRefEvent` |
| `AddPlayerToWorldEvent` | `com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent` |
| `DrainPlayerFromWorldEvent` | `com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent` |

### Entity Events
| Class | Import |
|-------|--------|
| `EntityEvent` | `com.hypixel.hytale.server.core.event.events.entity.EntityEvent` |
| `EntityRemoveEvent` | `com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent` |
| `LivingEntityInventoryChangeEvent` | `com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent` |

### Block Events
| Class | Import |
|-------|--------|
| `BreakBlockEvent` | `com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent` |
| `PlaceBlockEvent` | `com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent` |
| `DamageBlockEvent` | `com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent` |
| `UseBlockEvent` | `com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent` |

### Item Events
| Class | Import |
|-------|--------|
| `CraftRecipeEvent` | `com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent` |
| `DropItemEvent` | `com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent` |
| `InteractivelyPickupItemEvent` | `com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent` |

### Event System
| Class | Import |
|-------|--------|
| `EventBus` | `com.hypixel.hytale.event.EventBus` |
| `EventBusRegistry` | `com.hypixel.hytale.event.EventBusRegistry` |
| `AsyncEventBusRegistry` | `com.hypixel.hytale.event.AsyncEventBusRegistry` |
| `SyncEventBusRegistry` | `com.hypixel.hytale.event.SyncEventBusRegistry` |
| `EventRegistry` | `com.hypixel.hytale.event.EventRegistry` |
| `EventRegistration` | `com.hypixel.hytale.event.EventRegistration` |
| `EventPriority` | `com.hypixel.hytale.event.EventPriority` |
| `IEventBus` | `com.hypixel.hytale.event.IEventBus` |
| `IEvent` | `com.hypixel.hytale.event.IEvent` |
| `IAsyncEvent` | `com.hypixel.hytale.event.IAsyncEvent` |
| `IBaseEvent` | `com.hypixel.hytale.event.IBaseEvent` |
| `ICancellable` | `com.hypixel.hytale.event.ICancellable` |

---

## Components & Store (ECS)

### Core Component System
| Class | Import |
|-------|--------|
| `Component` | `com.hypixel.hytale.component.Component` |
| `ComponentType` | `com.hypixel.hytale.component.ComponentType` |
| `ComponentRegistry` | `com.hypixel.hytale.component.ComponentRegistry` |
| `ComponentRegistration` | `com.hypixel.hytale.component.ComponentRegistration` |
| `ComponentAccessor` | `com.hypixel.hytale.component.ComponentAccessor` |

### Store & References
| Class | Import |
|-------|--------|
| `Store` | `com.hypixel.hytale.component.Store` |
| `Ref` | `com.hypixel.hytale.component.Ref` |
| `Holder` | `com.hypixel.hytale.component.Holder` |
| `WeakComponentReference` | `com.hypixel.hytale.component.WeakComponentReference` |

### Archetype
| Class | Import |
|-------|--------|
| `Archetype` | `com.hypixel.hytale.component.Archetype` |
| `ArchetypeChunk` | `com.hypixel.hytale.component.ArchetypeChunk` |

### Resources
| Class | Import |
|-------|--------|
| `Resource` | `com.hypixel.hytale.component.Resource` |
| `ResourceType` | `com.hypixel.hytale.component.ResourceType` |
| `ResourceRegistration` | `com.hypixel.hytale.component.ResourceRegistration` |
| `IResourceStorage` | `com.hypixel.hytale.component.IResourceStorage` |

### Queries
| Class | Import |
|-------|--------|
| `Query` | `com.hypixel.hytale.component.query.Query` |
| `AndQuery` | `com.hypixel.hytale.component.query.AndQuery` |
| `OrQuery` | `com.hypixel.hytale.component.query.OrQuery` |
| `NotQuery` | `com.hypixel.hytale.component.query.NotQuery` |
| `AnyQuery` | `com.hypixel.hytale.component.query.AnyQuery` |

### Systems
| Class | Import |
|-------|--------|
| `System` | `com.hypixel.hytale.component.system.System` |
| `ISystem` | `com.hypixel.hytale.component.system.ISystem` |
| `SystemType` | `com.hypixel.hytale.component.SystemType` |
| `SystemGroup` | `com.hypixel.hytale.component.SystemGroup` |
| `StoreSystem` | `com.hypixel.hytale.component.system.StoreSystem` |
| `ArchetypeChunkSystem` | `com.hypixel.hytale.component.system.ArchetypeChunkSystem` |
| `QuerySystem` | `com.hypixel.hytale.component.system.QuerySystem` |
| `EventSystem` | `com.hypixel.hytale.component.system.EventSystem` |
| `RefSystem` | `com.hypixel.hytale.component.system.RefSystem` |

### ECS Events
| Class | Import |
|-------|--------|
| `EcsEvent` | `com.hypixel.hytale.component.system.EcsEvent` |
| `CancellableEcsEvent` | `com.hypixel.hytale.component.system.CancellableEcsEvent` |
| `EntityEventSystem` | `com.hypixel.hytale.component.system.EntityEventSystem` |

---

## Inventory & Items

### Inventory Core
| Class | Import |
|-------|--------|
| `Inventory` | `com.hypixel.hytale.server.core.inventory.Inventory` |
| `ItemStack` | `com.hypixel.hytale.server.core.inventory.ItemStack` |
| `ItemContext` | `com.hypixel.hytale.server.core.inventory.ItemContext` |

### Item Containers
| Class | Import |
|-------|--------|
| `ItemContainer` | `com.hypixel.hytale.server.core.inventory.container.ItemContainer` |
| `SimpleItemContainer` | `com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer` |
| `ItemStackItemContainer` | `com.hypixel.hytale.server.core.inventory.container.ItemStackItemContainer` |
| `CombinedItemContainer` | `com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer` |
| `DelegateItemContainer` | `com.hypixel.hytale.server.core.inventory.container.DelegateItemContainer` |
| `EmptyItemContainer` | `com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer` |

### Item Quantities
| Class | Import |
|-------|--------|
| `MaterialQuantity` | `com.hypixel.hytale.server.core.inventory.MaterialQuantity` |
| `ResourceQuantity` | `com.hypixel.hytale.server.core.inventory.ResourceQuantity` |

### Transactions
| Class | Import |
|-------|--------|
| `Transaction` | `com.hypixel.hytale.server.core.inventory.transaction.Transaction` |
| `ItemStackTransaction` | `com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction` |
| `MaterialTransaction` | `com.hypixel.hytale.server.core.inventory.transaction.MaterialTransaction` |
| `MoveTransaction` | `com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction` |
| `ClearTransaction` | `com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction` |

---

## Assets

### Asset Store
| Class | Import |
|-------|--------|
| `AssetStore` | `com.hypixel.hytale.assetstore.AssetStore` |
| `AssetRegistry` | `com.hypixel.hytale.assetstore.AssetRegistry` |
| `AssetMap` | `com.hypixel.hytale.assetstore.AssetMap` |
| `AssetPack` | `com.hypixel.hytale.assetstore.AssetPack` |
| `AssetHolder` | `com.hypixel.hytale.assetstore.AssetHolder` |

### Asset Utilities
| Class | Import |
|-------|--------|
| `JsonAsset` | `com.hypixel.hytale.assetstore.JsonAsset` |
| `DecodedAsset` | `com.hypixel.hytale.assetstore.DecodedAsset` |
| `RawAsset` | `com.hypixel.hytale.assetstore.RawAsset` |
| `AssetReferences` | `com.hypixel.hytale.assetstore.AssetReferences` |
| `MissingAssetException` | `com.hypixel.hytale.assetstore.MissingAssetException` |

### Asset Events
| Class | Import |
|-------|--------|
| `GenerateAssetsEvent` | `com.hypixel.hytale.assetstore.event.GenerateAssetsEvent` |
| `LoadedAssetsEvent` | `com.hypixel.hytale.assetstore.event.LoadedAssetsEvent` |
| `RegisterAssetStoreEvent` | `com.hypixel.hytale.assetstore.event.RegisterAssetStoreEvent` |
| `RemoveAssetStoreEvent` | `com.hypixel.hytale.assetstore.event.RemoveAssetStoreEvent` |

---

## Codecs

### Base Codecs
| Class | Import |
|-------|--------|
| `Codec` | `com.hypixel.hytale.codec.Codec` |
| `KeyedCodec` | `com.hypixel.hytale.codec.KeyedCodec` |
| `PrimitiveCodec` | `com.hypixel.hytale.codec.PrimitiveCodec` |
| `DirectDecodeCodec` | `com.hypixel.hytale.codec.DirectDecodeCodec` |
| `RawJsonCodec` | `com.hypixel.hytale.codec.RawJsonCodec` |

### Builder Codec
| Class | Import |
|-------|--------|
| `BuilderCodec` | `com.hypixel.hytale.codec.builder.BuilderCodec` |
| `BuilderField` | `com.hypixel.hytale.codec.builder.BuilderField` |

### Primitive Codecs
| Class | Import |
|-------|--------|
| `BooleanCodec` | `com.hypixel.hytale.codec.codecs.simple.BooleanCodec` |
| `IntegerCodec` | `com.hypixel.hytale.codec.codecs.simple.IntegerCodec` |
| `LongCodec` | `com.hypixel.hytale.codec.codecs.simple.LongCodec` |
| `FloatCodec` | `com.hypixel.hytale.codec.codecs.simple.FloatCodec` |
| `DoubleCodec` | `com.hypixel.hytale.codec.codecs.simple.DoubleCodec` |
| `StringCodec` | `com.hypixel.hytale.codec.codecs.simple.StringCodec` |

### Collection Codecs
| Class | Import |
|-------|--------|
| `MapCodec` | `com.hypixel.hytale.codec.codecs.map.MapCodec` |
| `EnumMapCodec` | `com.hypixel.hytale.codec.codecs.map.EnumMapCodec` |
| `SetCodec` | `com.hypixel.hytale.codec.codecs.set.SetCodec` |
| `ArrayCodec` | `com.hypixel.hytale.codec.codecs.array.ArrayCodec` |

### Codec Exceptions
| Class | Import |
|-------|--------|
| `CodecException` | `com.hypixel.hytale.codec.exception.CodecException` |
| `CodecValidationException` | `com.hypixel.hytale.codec.exception.CodecValidationException` |

---

## Miscellaneous

### Interactions
| Class | Import |
|-------|--------|
| `InteractionManager` | `com.hypixel.hytale.server.core.entity.InteractionManager` |
| `InteractionChain` | `com.hypixel.hytale.server.core.entity.InteractionChain` |
| `InteractionContext` | `com.hypixel.hytale.server.core.entity.InteractionContext` |
| `InteractionEntry` | `com.hypixel.hytale.server.core.entity.InteractionEntry` |

### Persistent References
| Class | Import |
|-------|--------|
| `PersistentRef` | `com.hypixel.hytale.server.core.entity.reference.PersistentRef` |
| `InvalidatablePersistentRef` | `com.hypixel.hytale.server.core.entity.reference.InvalidatablePersistentRef` |
| `PersistentRefCount` | `com.hypixel.hytale.server.core.entity.reference.PersistentRefCount` |

### Nameplate & Effects
| Class | Import |
|-------|--------|
| `Nameplate` | `com.hypixel.hytale.server.core.entity.nameplate.Nameplate` |
| `NameplateSystems` | `com.hypixel.hytale.server.core.entity.nameplate.NameplateSystems` |
| `ExplosionConfig` | `com.hypixel.hytale.server.core.entity.ExplosionConfig` |
| `ExplosionUtils` | `com.hypixel.hytale.server.core.entity.ExplosionUtils` |
| `StatModifiersManager` | `com.hypixel.hytale.server.core.entity.StatModifiersManager` |
