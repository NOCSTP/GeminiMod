// src/main/java/com/chaoticdungeons/chaoticdungeons/capabilities/DungeonGenerationTracker.java
package com.chaoticdungeons.chaoticdungeons.capabilities;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Capability for tracking previously generated dungeon positions within a level.
 * This ensures that new dungeons are spawned at a minimum distance from existing ones.
 * Implements INBTSerializable to allow saving and loading with the world.
 */
public class DungeonGenerationTracker implements INBTSerializable<ListTag> {

    // Using a HashSet for efficient O(1) average time complexity for additions and lookups.
    private final Set<BlockPos> generatedDungeonPositions = new HashSet<>();
    private boolean isDirty = false; // Flag to indicate if data has changed and needs saving

    /**
     * Adds a new generated dungeon position to the tracker.
     *
     * @param pos The BlockPos of the newly generated dungeon.
     */
    public void addGeneratedDungeonPosition(BlockPos pos) {
        if (generatedDungeonPositions.add(pos)) {
            isDirty = true; // Mark as dirty if a new position was added
            ChaoticDungeons.LOGGER.debug("DungeonGenerationTracker: Added new dungeon position: {}. Total: {}", pos, generatedDungeonPositions.size());
        }
    }

    /**
     * Returns an unmodifiable set of all currently tracked dungeon positions.
     *
     * @return A Set of BlockPos representing generated dungeons.
     */
    public Set<BlockPos> getGeneratedDungeonPositions() {
        return Collections.unmodifiableSet(generatedDungeonPositions);
    }

    /**
     * Checks if the tracker's data has been modified and needs to be saved.
     *
     * @return True if data is dirty, false otherwise.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Resets the dirty flag after saving.
     */
    public void setClean() {
        isDirty = false;
        ChaoticDungeons.LOGGER.debug("DungeonGenerationTracker: Cleaned dirty flag.");
    }

    /**
     * Serializes the set of generated dungeon positions to a ListTag for NBT saving.
     * Each BlockPos is saved as a CompoundTag using NbtUtils.writeBlockPos.
     *
     * @return A ListTag containing the serialized BlockPos objects.
     */
    @Override
    public ListTag serializeNBT() {
        ListTag listTag = new ListTag();
        for (BlockPos pos : generatedDungeonPositions) {
            listTag.add(NbtUtils.writeBlockPos(pos));
        }
        ChaoticDungeons.LOGGER.debug("DungeonGenerationTracker: Serialized {} dungeon positions.", generatedDungeonPositions.size());
        setClean(); // Mark as clean after serialization for saving
        return listTag;
    }

    /**
     * Deserializes a ListTag back into the set of generated dungeon positions.
     * Each CompoundTag within the ListTag is read back into a BlockPos using NbtUtils.readBlockPos.
     *
     * @param nbt The ListTag containing the serialized BlockPos objects.
     */
    @Override
    public void deserializeNBT(ListTag nbt) {
        generatedDungeonPositions.clear(); // Clear existing data before loading
        for (Tag tag : nbt) {
            if (tag instanceof CompoundTag compoundTag) {
                generatedDungeonPositions.add(NbtUtils.readBlockPos(compoundTag));
            }
        }
        ChaoticDungeons.LOGGER.debug("DungeonGenerationTracker: Deserialized {} dungeon positions.", generatedDungeonPositions.size());
    }
}