// src/main/java/com/chaoticdungeons/chaoticdungeons/capabilities/DungeonGenerationTrackerProvider.java
package com.chaoticdungeons.chaoticdungeons.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider class for the DungeonGenerationTracker capability.
 * This class handles the instantiation and serialization/deserialization
 * of the DungeonGenerationTracker when attached to a Level.
 */
public class DungeonGenerationTrackerProvider implements ICapabilitySerializable<ListTag> {

    // Unique identifier for this capability.
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("chaotic_dungeons", "dungeon_tracker");

    // The actual capability instance, lazily initialized.
    public static Capability<DungeonGenerationTracker> DUNGEON_GENERATION_TRACKER_CAPABILITY = null; // Will be set by deferred register

    // The actual instance of the DungeonGenerationTracker.
    private final DungeonGenerationTracker instance = new DungeonGenerationTracker();
    // LazyOptional for the capability, allowing for nullable and lazy access.
    private final LazyOptional<DungeonGenerationTracker> optional = LazyOptional.of(() -> instance);

    /**
     * Returns the lazy optional of the DungeonGenerationTracker capability.
     *
     * @param cap The capability to retrieve.
     * @param side The direction (side) from which the capability is being accessed (can be null).
     * @param <T> The type of the capability.
     * @return A LazyOptional containing the DungeonGenerationTracker instance if the capability matches, otherwise empty.
     */
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Return the instance if the requested capability matches our DungeonGenerationTracker capability.
        return cap == DUNGEON_GENERATION_TRACKER_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    /**
     * Serializes the DungeonGenerationTracker's data into a ListTag.
     * This is called by Forge when the world is saved.
     *
     * @return A ListTag representing the serialized state of the tracker.
     */
    @Override
    public ListTag serializeNBT() {
        return instance.serializeNBT();
    }

    /**
     * Deserializes data from a ListTag into the DungeonGenerationTracker.
     * This is called by Forge when the world is loaded.
     *
     * @param nbt The ListTag containing the serialized tracker data.
     */
    @Override
    public void deserializeNBT(ListTag nbt) {
        instance.deserializeNBT(nbt);
    }
}