// src/main/java/com/chaoticdungeons/chaoticdungeons/handlers/StructureSummoner.java
package com.chaoticdungeons.chaoticdungeons.handlers;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.chaoticdungeons.chaoticdungeons.dungeons.DungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;
import java.util.Random;

/**
 * Handles the loading and summoning of Minecraft structures (NBT files) into the world.
 */
public class StructureSummoner {

    private final Random random = new Random();

    /**
     * Summons a Minecraft structure (NBT file) into the given ServerLevel at the specified spawn position.
     * The structure is chosen from the provided DungeonData.
     *
     * @param level The ServerLevel to summon the structure in.
     * @param spawnPos The BlockPos where the structure's origin (0,0,0) will be placed.
     * @param dungeonData The DungeonData object containing the structure's resource location.
     * @return True if the structure was successfully summoned, false otherwise.
     */
    public boolean summonStructure(ServerLevel level, BlockPos spawnPos, DungeonData dungeonData) {
        if (dungeonData.structure() == null || dungeonData.structure().isEmpty()) {
            ChaoticDungeons.LOGGER.error("StructureSummoner: DungeonData for summoning is missing a structure path.");
            return false;
        }

        ResourceLocation structureLocation = new ResourceLocation(dungeonData.structure());
        StructureTemplateManager templateManager = level.getStructureManager();

        Optional<StructureTemplate> structureOptional = templateManager.get(structureLocation);

        if (structureOptional.isEmpty()) {
            ChaoticDungeons.LOGGER.error("StructureSummoner: Failed to load structure template: {}. Ensure the .nbt file exists in 'data/{}/structures/'", structureLocation, structureLocation.getNamespace());
            return false;
        }

        StructureTemplate structure = structureOptional.get();

        // Define placement settings.
        // Rotation can be random for variety.
        Rotation rotation = Rotation.getRandom(RandomSource.create());
        // Mirroring can also be random.
        // Mirror mirror = Mirror.values()[random.nextInt(Mirror.values().length)];

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                // .setMirror(mirror) // Uncomment if mirroring is desired
                .setIgnoreEntities(false) // Whether to include entities defined in the structure
                .setKnownShape(false); // Optimization, set to true if you know the structure's shape and collisions will be handled elsewhere.

        // Place the structure.
        // The `true` argument means 'do not add entities from the structure directly'.
        // If you want entities from your structure NBT (e.g., spawners), set it to `false`.
        // The default `place` method is often sufficient.
        boolean placed = structure.placeInWorld(level, spawnPos, spawnPos, settings, RandomSource.create(), 2);

        if (!placed) {
            ChaoticDungeons.LOGGER.error("StructureSummoner: Failed to place structure {} at {}.", structureLocation, spawnPos);
        } else {
            ChaoticDungeons.LOGGER.info("StructureSummoner: Successfully placed structure {} at {} with rotation {}.", structureLocation, spawnPos, rotation);
        }

        return placed;
    }
}