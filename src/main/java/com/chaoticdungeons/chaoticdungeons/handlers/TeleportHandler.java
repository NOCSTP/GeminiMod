// src/main/java/com/chaoticdungeons/chaoticdungeons/handlers/TeleportHandler.java
package com.chaoticdungeons.chaoticdungeons.handlers;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Handles teleportation of players into generated dungeons.
 * It attempts to find a safe and suitable teleportation point within the dungeon's boundaries.
 */
public class TeleportHandler {

    private final BlockPos dungeonOrigin; // The BlockPos where the dungeon structure was placed.

    /**
     * Constructor for TeleportHandler.
     *
     * @param dungeonOrigin The BlockPos representing the origin point of the generated dungeon structure.
     */
    public TeleportHandler(BlockPos dungeonOrigin) {
        this.dungeonOrigin = dungeonOrigin;
    }

    /**
     * Handles teleporting a player to a safe spot within the generated dungeon.
     * It iterates through a small area around the dungeon origin to find a clear 2-block high space.
     *
     * @param player The ServerPlayer to teleport.
     * @param level The ServerLevel where the dungeon is located.
     */
    public void handleTeleport(ServerPlayer player, ServerLevel level) {
        ChaoticDungeons.LOGGER.info("Attempting to teleport player {} to dungeon at origin {}.", player.getName().getString(), dungeonOrigin);

        // Define a search area around the dungeon origin to find a safe spot
        // A common practice is to place a specific "spawn" block in your structure and teleport to that.
        // For this generic case, we'll search.
        int searchRadius = 5; // Search 5 blocks around the origin
        BlockPos safeTeleportPos = null;

        // Iterate through a small cube around the dungeon origin to find a safe spot
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = 0; y <= searchRadius; y++) { // Search upwards from origin
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos candidatePos = dungeonOrigin.offset(x, y, z);

                    // Check if the two blocks above the candidate position are air or replaceable
                    BlockState lowerBlock = level.getBlockState(candidatePos);
                    BlockState upperBlock = level.getBlockState(candidatePos.above());
                    BlockState groundBlock = level.getBlockState(candidatePos.below());

                    // A "safe" spot means:
                    // 1. The two blocks at candidatePos and candidatePos.above() are air or replaceable.
                    // 2. The ground block below is solid (something to stand on).
                    // 3. No fluid at the player's head or foot level.
                    if ((lowerBlock.isAir() || lowerBlock.canBeReplaced()) &&
                            (upperBlock.isAir() || upperBlock.canBeReplaced()) &&
                            groundBlock.isSolid() && // FIX: Changed isSolidBlocking() to isSolid()
                            !lowerBlock.getFluidState().is(Fluids.WATER) && // Not in water
                            !lowerBlock.getFluidState().is(Fluids.LAVA)
                    ) {
                        safeTeleportPos = candidatePos;
                        break;
                    }
                }
                if (safeTeleportPos != null) break;
            }
            if (safeTeleportPos != null) break;
        }

        if (safeTeleportPos != null) {
            // Teleport the player. Use teleportTo for safe teleportation.
            player.teleportTo(level, safeTeleportPos.getX() + 0.5, safeTeleportPos.getY(), safeTeleportPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            ChaoticDungeons.LOGGER.info("Player {} successfully teleported to safe spot: {}", player.getName().getString(), safeTeleportPos);
        } else {
            ChaoticDungeons.LOGGER.warn("Could not find a perfect safe teleport spot for player {} at dungeon origin {}. Teleporting to origin directly.", player.getName().getString(), dungeonOrigin);
            // Fallback: Teleport to the origin block, might place them inside a block or liquid
            player.teleportTo(level, dungeonOrigin.getX() + 0.5, dungeonOrigin.getY(), dungeonOrigin.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
    }
}