// src/main/java/com/chaoticdungeons/chaoticdungeons/selectors/PositionSelector.java
package com.chaoticdungeons.chaoticdungeons.selectors;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.chaoticdungeons.chaoticdungeons.capabilities.DungeonGenerationTracker;
import com.chaoticdungeons.chaoticdungeons.capabilities.DungeonGenerationTrackerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;
import java.util.Random;

/**
 * Utility class for selecting suitable positions for dungeon generation.
 * Ensures that generated dungeons are spaced apart to prevent overlap and maintain uniqueness.
 * Uses a Forge Capability to persist previously generated dungeon locations.
 */
public class PositionSelector {

    private static final int MIN_DISTANCE_BETWEEN_DUNGEONS = 500; // Blocks
    private static final int MAX_ATTEMPTS = 50; // Max attempts to find a suitable position
    private static final int SEARCH_RADIUS_AROUND_SPAWN = 20000; // Search radius for initial spawn points (large to ensure variety)

    private final Random random = new Random();

    /**
     * Constructor. Registers event listeners for capability handling.
     */
    public PositionSelector() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Event listener to register the DungeonGenerationTracker capability.
     * This must be done once per mod.
     *
     * @param event The RegisterCapabilitiesEvent.
     */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(DungeonGenerationTracker.class);
        ChaoticDungeons.LOGGER.debug("Registered DungeonGenerationTracker capability.");
    }

    /**
     * Event listener to attach the DungeonGenerationTracker capability to Level objects.
     * This ensures each level has its own tracker.
     *
     * @param event The AttachCapabilitiesEvent for a Level.
     */
    @SubscribeEvent
    public void onAttachCapabilitiesEvent(AttachCapabilitiesEvent<Level> event) {
        if (event.getObject() instanceof ServerLevel) { // Only attach to server-side levels
            if (!event.getObject().getCapability(DungeonGenerationTrackerProvider.DUNGEON_GENERATION_TRACKER_CAPABILITY).isPresent()) {
                event.addCapability(DungeonGenerationTrackerProvider.IDENTIFIER, new DungeonGenerationTrackerProvider());
                ChaoticDungeons.LOGGER.debug("Attached DungeonGenerationTracker capability to level {}.", event.getObject().dimension().location());
            }
        }
    }

    /**
     * Event listener for Level save events. Ensures the capability data is saved.
     *
     * @param event The LevelEvent.Save event.
     */
    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getCapability(DungeonGenerationTrackerProvider.DUNGEON_GENERATION_TRACKER_CAPABILITY).ifPresent(tracker -> {
                // The capability's NBT handler should manage saving. No explicit call needed here
                // other than ensuring the capability object is marked as 'dirty' if its state changes.
                // The `saveAdditional` method in the capability's storage will be called by Forge.
                ChaoticDungeons.LOGGER.debug("Saving DungeonGenerationTracker data for level {}.", serverLevel.dimension().location());
            });
        }
    }

    /**
     * Selects a suitable BlockPos for dungeon generation in the given level.
     * The position is chosen to be at least MIN_DISTANCE_BETWEEN_DUNGEONS away from any
     * previously generated dungeon and a safe spot on the surface.
     *
     * @param level The ServerLevel to select a position in.
     * @return An Optional containing the BlockPos if a suitable position is found, otherwise empty.
     */
    public Optional<BlockPos> selectPosition(ServerLevel level) {
        // Get the DungeonGenerationTracker capability for this level
        Optional<DungeonGenerationTracker> trackerOptional = level.getCapability(DungeonGenerationTrackerProvider.DUNGEON_GENERATION_TRACKER_CAPABILITY).resolve();

        if (trackerOptional.isEmpty()) {
            ChaoticDungeons.LOGGER.error("PositionSelector: Could not retrieve DungeonGenerationTracker capability for level {}. Cannot select position.", level.dimension().location());
            return Optional.empty();
        }

        DungeonGenerationTracker tracker = trackerOptional.get();
        BlockPos spawnPoint = level.getSharedSpawnPos(); // Use world spawn as a reference
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            attempts++;
            // Generate a random X and Z coordinate far from spawn but within a reasonable range
            int x = spawnPoint.getX() + random.nextInt(SEARCH_RADIUS_AROUND_SPAWN * 2) - SEARCH_RADIUS_AROUND_SPAWN;
            int z = spawnPoint.getZ() + random.nextInt(SEARCH_RADIUS_AROUND_SPAWN * 2) - SEARCH_RADIUS_AROUND_SPAWN;

            // Find the highest solid block at this X, Z
            // Use getHighestData for performance on potentially loaded chunks
            // Or getChunk at first and then getHeight to avoid chunk loading on every call
            BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));

            // Check if the ground is too high or too low, or if it's liquid
            if (groundPos.getY() < level.getMinBuildHeight() + 10 || groundPos.getY() > level.getMaxBuildHeight() - 10) {
                continue; // Skip if too close to world bounds
            }
            if (level.getBlockState(groundPos).getFluidState().is(Fluids.WATER) || level.getBlockState(groundPos).getFluidState().is(Fluids.LAVA)) {
                continue; // Skip if on water/lava
            }

            // Move up a few blocks to ensure space for dungeon entrance / spawn
            BlockPos proposedPos = groundPos.above(2);

            // Check distance to all existing dungeons
            boolean tooClose = false;
            for (BlockPos existingDungeonPos : tracker.getGeneratedDungeonPositions()) {
                if (existingDungeonPos.distManhattan(proposedPos) < MIN_DISTANCE_BETWEEN_DUNGEONS) {
                    tooClose = true;
                    ChaoticDungeons.LOGGER.debug("Proposed dungeon position {} is too close to existing dungeon {}.", proposedPos, existingDungeonPos);
                    break;
                }
            }

            if (!tooClose) {
                // Ensure the chunk is loaded before marking the position
                if (!level.hasChunkAt(proposedPos)) {
                    // Force chunk load to accurately check surrounding area and add to tracker
                    // This can be a performance concern if done excessively.
                    // For now, assume a chunk will be loaded during structure generation.
                    // The primary check is against the *tracker*, not dynamically loaded chunks.
                }

                tracker.addGeneratedDungeonPosition(proposedPos); // Mark this position as used
                ChaoticDungeons.LOGGER.info("PositionSelector: Found suitable dungeon spawn position after {} attempts: {}", attempts, proposedPos);
                return Optional.of(proposedPos);
            }
        }

        ChaoticDungeons.LOGGER.warn("PositionSelector: Failed to find a suitable dungeon spawn position after {} attempts.", MAX_ATTEMPTS);
        return Optional.empty();
    }
}