// src/main/java/com/chaoticdungeons/chaoticdungeons/blockentities/GateBlockEntity.java
package com.chaoticdungeons.chaoticdungeons.blockentities;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.chaoticdungeons.chaoticdungeons.dungeons.DungeonData;
import com.chaoticdungeons.chaoticdungeons.handlers.StructureSummoner;
import com.chaoticdungeons.chaoticdungeons.handlers.TeleportHandler;
import com.chaoticdungeons.chaoticdungeons.items.keys.BaseKeyItem;
import com.chaoticdungeons.chaoticdungeons.registration.ModRegisters;
import com.chaoticdungeons.chaoticdungeons.selectors.DungeonSelector;
import com.chaoticdungeons.chaoticdungeons.selectors.PositionSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion; // Ensure this import is present for Explosion.BlockInteraction
import net.minecraft.world.level.Explosion.BlockInteraction; // Explicitly import BlockInteraction
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Block Entity for Gate Blocks.
 * This entity stores the state of an activated gate, including the chosen dungeon data,
 * the generated dungeon's coordinates, and a timer for its active state.
 * It handles the activation logic, dungeon generation, player teleportation, and self-destruction.
 */
public class GateBlockEntity extends BlockEntity {

    private static final int ACTIVATION_TIMER_SECONDS = 20;
    private static final int TICKS_PER_SECOND = 20;

    private boolean isActive;
    private int activationTimer; // In ticks
    private BlockPos generatedDungeonPos;
    private String selectedDungeonStructure;
    private String selectedDungeonType;
    private int selectedDungeonDifficulty;

    public GateBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(ModRegisters.GATE_BLOCK_ENTITY.get(), p_155229_, p_155230_);
        this.isActive = false;
        this.activationTimer = 0;
        this.generatedDungeonPos = null;
        this.selectedDungeonStructure = "";
        this.selectedDungeonType = "";
        this.selectedDungeonDifficulty = 0;
    }

    /**
     * Ticks the GateBlockEntity. This method is called every server tick.
     * It manages the activation timer and triggers the block's explosion when the timer runs out.
     *
     * @param level The level (world) the block entity is in.
     * @param pos The position of the block entity.
     * @param state The block state of the block entity.
     * @param blockEntity The instance of the GateBlockEntity.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, GateBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            if (blockEntity.isActive) {
                blockEntity.activationTimer--;
                if (blockEntity.activationTimer <= 0) {
                    blockEntity.explodeBlock();
                    blockEntity.isActive = false; // Deactivate after explosion
                    blockEntity.setChanged(); // Mark for saving - FIX: Changed setDirty() to setChanged()
                    ChaoticDungeons.LOGGER.info("GateBlock at {} timer expired, triggering explosion.", pos);
                }
            }
        }
    }

    /**
     * Attempts to activate the gate block.
     * This method is called when a player interacts with the gate block using a key.
     * It handles key validation, dungeon selection, generation, and player teleportation.
     *
     * @param player The server player interacting with the block.
     * @param key The key item used by the player.
     * @return True if activation was successful, false otherwise.
     */
    public boolean activate(ServerPlayer player, BaseKeyItem key) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // If already active and the player uses the same key, teleport them to the existing dungeon.
        if (isActive) {
            // Check if the key matches the currently active dungeon's type and difficulty requirements
            if (key.getOpensDungeonType().equals(this.selectedDungeonType) && key.getOpensDungeonDifficulty() >= this.selectedDungeonDifficulty) {
                ChaoticDungeons.LOGGER.info("Player {} re-activating existing gate at {}. Teleporting to dungeon at {}", player.getName().getString(), getBlockPos(), this.generatedDungeonPos);
                // Teleport player to the already generated dungeon
                TeleportHandler teleportHandler = new TeleportHandler(this.generatedDungeonPos);
                teleportHandler.handleTeleport(player, serverLevel);
                return true;
            } else {
                ChaoticDungeons.LOGGER.warn("Player {} tried to re-activate gate at {} with incompatible key ({}). Requires type: {}, difficulty: {}",
                        player.getName().getString(), getBlockPos(), key.getOpensDungeonType(), this.selectedDungeonType, this.selectedDungeonDifficulty);
                // Optionally send a message to the player
                // player.sendSystemMessage(Component.literal("This gate requires a different key."), true);
                return false;
            }
        }

        // --- First-time activation logic ---
        ChaoticDungeons.LOGGER.info("Gate block at {} is being activated by player {} with key type: {}, difficulty: {}",
                getBlockPos(), player.getName().getString(), key.getOpensDungeonType(), key.getOpensDungeonDifficulty());

        // 1. Select suitable dungeons based on key type and difficulty
        DungeonSelector dungeonSelector = new DungeonSelector();
        List<DungeonData> availableDungeons = dungeonSelector.selectDungeons(key.getOpensDungeonType(), key.getOpensDungeonDifficulty());

        if (availableDungeons.isEmpty()) {
            ChaoticDungeons.LOGGER.warn("No suitable dungeons found for key type '{}' and difficulty {}.", key.getOpensDungeonType(), key.getOpensDungeonDifficulty());
            // Optionally send a message to the player: "No dungeons found for this key!"
            return false;
        }

        // Randomly select one dungeon from the filtered list
        DungeonData chosenDungeon = availableDungeons.get(new Random().nextInt(availableDungeons.size()));
        ChaoticDungeons.LOGGER.debug("Selected dungeon: {} (Type: {}, Difficulty: {})", chosenDungeon.structure(), chosenDungeon.type(), chosenDungeon.difficulty());

        // 2. Select a suitable position for dungeon generation
        PositionSelector positionSelector = new PositionSelector();
        Optional<BlockPos> selectedPos = positionSelector.selectPosition(serverLevel);

        if (selectedPos.isEmpty()) {
            ChaoticDungeons.LOGGER.error("Failed to find a suitable position for dungeon generation near {}.", getBlockPos());
            // Optionally send a message to the player: "Could not find a safe place for a dungeon!"
            return false;
        }

        BlockPos dungeonSpawnPos = selectedPos.get();
        ChaoticDungeons.LOGGER.info("Selected dungeon spawn position: {}", dungeonSpawnPos);

        // 3. Summon the structure
        StructureSummoner structureSummoner = new StructureSummoner();
        boolean structureSummoned = structureSummoner.summonStructure(serverLevel, dungeonSpawnPos, chosenDungeon);

        if (!structureSummoned) {
            ChaoticDungeons.LOGGER.error("Failed to summon structure {} at {}.", chosenDungeon.structure(), dungeonSpawnPos);
            // Optionally send a message to the player: "Dungeon generation failed!"
            return false;
        }
        ChaoticDungeons.LOGGER.info("Successfully summoned structure {} at {}", chosenDungeon.structure(), dungeonSpawnPos);


        // 4. Teleport the activating player
        TeleportHandler teleportHandler = new TeleportHandler(dungeonSpawnPos);
        teleportHandler.handleTeleport(player, serverLevel);
        ChaoticDungeons.LOGGER.info("Player {} teleported to dungeon at {}", player.getName().getString(), dungeonSpawnPos);

        // 5. Update BlockEntity state and start timer
        this.isActive = true;
        this.activationTimer = ACTIVATION_TIMER_SECONDS * TICKS_PER_SECOND;
        this.generatedDungeonPos = dungeonSpawnPos;
        this.selectedDungeonStructure = chosenDungeon.structure();
        this.selectedDungeonType = chosenDungeon.type();
        this.selectedDungeonDifficulty = chosenDungeon.difficulty();
        this.setChanged(); // Mark chunk for saving
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3); // Sync with clients

        return true;
    }

    /**
     * Triggers an explosion at the block's location.
     * This method is called when the activation timer runs out.
     */
    private void explodeBlock() {
        if (level instanceof ServerLevel serverLevel) {
            ChaoticDungeons.LOGGER.info("Exploding GateBlock at {}.", getBlockPos());
            // Create an explosion. Adjust power as needed. BlockInteraction.DESTROY prevents drops.
            // Using a simple ExplosionDamageCalculator allows for fine-tuning.
            // FIX: Ensure correct BlockInteraction enum is used.
            serverLevel.explode(null, null, new ExplosionDamageCalculator(), getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, 4.0F, false, Level.ExplosionInteraction.BLOCK);

            // Immediately remove the block entity and block state to ensure it's gone
            serverLevel.removeBlockEntity(getBlockPos());
            serverLevel.setBlockAndUpdate(getBlockPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * Reads NBT data into the BlockEntity. Used for loading state from disk.
     *
     * @param nbt The CompoundTag containing the NBT data.
     */
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.isActive = nbt.getBoolean("IsActive");
        this.activationTimer = nbt.getInt("ActivationTimer");
        if (nbt.contains("GeneratedDungeonX")) { // Check for presence of all position components
            this.generatedDungeonPos = new BlockPos(nbt.getInt("GeneratedDungeonX"), nbt.getInt("GeneratedDungeonY"), nbt.getInt("GeneratedDungeonZ"));
        } else {
            this.generatedDungeonPos = null; // Clear if not found
        }
        this.selectedDungeonStructure = nbt.getString("SelectedDungeonStructure");
        this.selectedDungeonType = nbt.getString("SelectedDungeonType");
        this.selectedDungeonDifficulty = nbt.getInt("SelectedDungeonDifficulty");

        ChaoticDungeons.LOGGER.debug("GateBlockEntity at {} loaded: isActive={}, timer={}", getBlockPos(), isActive, activationTimer);
    }

    /**
     * Writes the BlockEntity's data to NBT. Used for saving state to disk.
     *
     * @param nbt The CompoundTag to write data to.
     */
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("IsActive", this.isActive);
        nbt.putInt("ActivationTimer", this.activationTimer);
        if (this.generatedDungeonPos != null) {
            nbt.putInt("GeneratedDungeonX", this.generatedDungeonPos.getX());
            nbt.putInt("GeneratedDungeonY", this.generatedDungeonPos.getY());
            nbt.putInt("GeneratedDungeonZ", this.generatedDungeonPos.getZ());
        }
        nbt.putString("SelectedDungeonStructure", this.selectedDungeonStructure);
        nbt.putString("SelectedDungeonType", this.selectedDungeonType);
        nbt.putInt("SelectedDungeonDifficulty", this.selectedDungeonDifficulty);

        ChaoticDungeons.LOGGER.debug("GateBlockEntity at {} saved: isActive={}, timer={}", getBlockPos(), isActive, activationTimer);
    }

    /**
     * Gets the update packet for client synchronization.
     *
     * @return The ClientboundBlockEntityDataPacket.
     */
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Gets the update tag for client synchronization.
     *
     * @return The CompoundTag containing update data.
     */
    @Override
    public CompoundTag getUpdateTag() {
        // Return the full NBT data for synchronization
        return saveWithFullMetadata();
    }

    /**
     * Handles the client-side update from the server's update packet.
     *
     * @param tag The CompoundTag received from the server.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        // This is called on the client when the server sends a BlockEntity update.
        load(tag); // Load the received NBT data
    }

    // --- Getters for Block Entity State (can be used for rendering or UI if needed) ---
    public boolean isActive() {
        return isActive;
    }

    public int getActivationTimer() {
        return activationTimer;
    }

    public BlockPos getGeneratedDungeonPos() {
        return generatedDungeonPos;
    }

    public String getSelectedDungeonStructure() {
        return selectedDungeonStructure;
    }

    public String getSelectedDungeonType() {
        return selectedDungeonType;
    }

    public int getSelectedDungeonDifficulty() {
        return selectedDungeonDifficulty;
    }
}