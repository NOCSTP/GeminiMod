// src/main/java/com/chaoticdungeons/chaoticdungeons/blocks/gateblocks/GateBlock.java
package com.chaoticdungeons.chaoticdungeons.blocks.gateblocks;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.chaoticdungeons.chaoticdungeons.blockentities.GateBlockEntity;
import com.chaoticdungeons.chaoticdungeons.items.keys.BaseKeyItem;
import com.chaoticdungeons.chaoticdungeons.registration.ModRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all gate blocks in Chaotic Dungeons.
 * This class provides common properties and interaction logic for gate blocks,
 * including associating a BlockEntity and handling player interaction for activation.
 */
public abstract class GateBlock extends BaseEntityBlock {

    /**
     * Constructor for the GateBlock.
     * Sets default block properties suitable for gate blocks.
     */
    protected GateBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK) // Example: Black map color
                .strength(4.0F, 1200.0F) // High strength, explosion resistance
                .sound(SoundType.NETHERITE_BLOCK) // Example: Netherite sound
                .noOcclusion() // Allows for custom rendering if needed
                .lightLevel((state) -> 5)); // Emits some light
    }

    /**
     * Determines the render shape of the block. For a BlockEntity, typically invisible or custom.
     *
     * @param p_49232_ The block state.
     * @return The render shape.
     */
    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL; // Use a standard model for now. Can be INVISIBLE if only the BE matters visually.
    }

    /**
     * Called when a player right-clicks on the block.
     * This method handles the core interaction logic for activating the gate.
     *
     * @param p_49237_ The block state.
     * @param level The current level (world).
     * @param pos The position of the block.
     * @param player The player interacting with the block.
     * @param hand The hand used by the player.
     * @param hitResult The hit result of the interaction.
     * @return The result of the interaction (success, pass, fail).
     */
    @Override
    public InteractionResult use(BlockState p_49237_, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) { // Server side only
            ItemStack heldItem = player.getItemInHand(hand);

            if (level.getBlockEntity(pos) instanceof GateBlockEntity gateBlockEntity) {
                // Check if the held item is a key
                if (heldItem.getItem() instanceof BaseKeyItem key) {
                    ChaoticDungeons.LOGGER.debug("Player {} used key {} on gate block at {}", player.getName().getString(), heldItem.getItem().getDescriptionId(), pos);
                    // Attempt to activate the gate block entity with the key and player
                    return gateBlockEntity.activate((ServerPlayer) player, key) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                } else {
                    ChaoticDungeons.LOGGER.debug("Player {} clicked gate block at {} with non-key item: {}", player.getName().getString(), pos, heldItem.getItem().getDescriptionId());
                }
            } else {
                ChaoticDungeons.LOGGER.warn("GateBlock at {} has no GateBlockEntity! This is an error.", pos);
            }
        }
        return InteractionResult.CONSUME; // Consume the action on client to prevent double processing
    }

    /**
     * Creates a new BlockEntity for this block.
     *
     * @param pos The position of the block.
     * @param state The block state.
     * @return A new instance of GateBlockEntity.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateBlockEntity(pos, state);
    }

    /**
     * Gets the ticker for the BlockEntity. This is crucial for ticking BlockEntities.
     *
     * @param <T> The type of the BlockEntity.
     * @param <A> The type of the expected BlockEntity.
     * @param <E> The type of the BlockEntity type.
     * @param level The level (world).
     * @param blockEntityType The BlockEntityType of the current block.
     * @return An appropriate BlockEntityTicker, or null if no ticking is needed.
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockEntityType, BlockEntityType<T> p_153212_) {
        // Only tick on the server side
        return createTickerHelper(p_153212_, ModRegisters.GATE_BLOCK_ENTITY.get(), GateBlockEntity::tick);
    }

    /**
     * Called when the block is removed. Ensures the associated BlockEntity is also cleaned up.
     *
     * @param state The current block state.
     * @param level The level (world).
     * @param pos The position of the block.
     * @param newState The new block state.
     * @param isMoving Whether the block is moving.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof GateBlockEntity gateBlockEntity) {
                // If there's any important cleanup, do it here.
                // For now, just logging.
                ChaoticDungeons.LOGGER.debug("GateBlock at {} being removed.", pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}