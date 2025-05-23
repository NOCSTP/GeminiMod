// src/main/java/com/chaoticdungeons/chaoticdungeons/registration/ModRegisters.java
package com.chaoticdungeons.chaoticdungeons.registration;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.chaoticdungeons.chaoticdungeons.blockentities.GateBlockEntity;
import com.chaoticdungeons.chaoticdungeons.blocks.gateblocks.*;
import com.chaoticdungeons.chaoticdungeons.items.keys.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * A central registration class for all mod blocks, items, and block entity types.
 * This class uses Forge's DeferredRegister system for organized and automatic registration.
 */
public class ModRegisters {
    /**
     * DeferredRegister for blocks. Blocks are physical entities in the world.
     */
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ChaoticDungeons.MOD_ID);
    /**
     * DeferredRegister for items. Items are things players can hold or use.
     */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChaoticDungeons.MOD_ID);
    /**
     * DeferredRegister for block entity types. Block entities store complex data for blocks.
     */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ChaoticDungeons.MOD_ID);

    // --- Block Registry Objects ---
    public static final RegistryObject<Block> BASIC_GATE_BLOCK = BLOCKS.register("basic_gate_block", BasicGateBlock::new);
    public static final RegistryObject<Block> CAVE_GATE_BLOCK = BLOCKS.register("cave_gate_block", CaveGateBlock::new);
    public static final RegistryObject<Block> SEWERAGE_GATE_BLOCK = BLOCKS.register("sewerage_gate_block", SewerageGateBlock::new);
    public static final RegistryObject<Block> DARK_GATE_BLOCK = BLOCKS.register("dark_gate_block", DarkGateBlock::new);

    // --- Item Registry Objects (and BlockItems for Gate Blocks) ---
    public static final RegistryObject<Item> BASIC_GATE_BLOCK_ITEM = registerBlockItem("basic_gate_block", BASIC_GATE_BLOCK);
    public static final RegistryObject<Item> CAVE_GATE_BLOCK_ITEM = registerBlockItem("cave_gate_block", CAVE_GATE_BLOCK);
    public static final RegistryObject<Item> SEWERAGE_GATE_BLOCK_ITEM = registerBlockItem("sewerage_gate_block", SEWERAGE_GATE_BLOCK);
    public static final RegistryObject<Item> DARK_GATE_BLOCK_ITEM = registerBlockItem("dark_gate_block", DARK_GATE_BLOCK);

    public static final RegistryObject<Item> BRONZE_KEY = ITEMS.register("bronze_key", BronzeKey::new);
    public static final RegistryObject<Item> IRON_KEY = ITEMS.register("iron_key", IronKey::new);
    public static final RegistryObject<Item> GOLDEN_KEY = ITEMS.register("golden_key", GoldenKey::new);
    public static final RegistryObject<Item> DIAMOND_KEY = ITEMS.register("diamond_key", DiamondKey::new);
    public static final RegistryObject<Item> SCORBIUM_KEY = ITEMS.register("scorbium_key", ScorbiumKey::new);

    // --- Block Entity Type Registry Objects ---
    public static final RegistryObject<BlockEntityType<GateBlockEntity>> GATE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("gate_block_entity",
            () -> BlockEntityType.Builder.of(GateBlockEntity::new,
                    BASIC_GATE_BLOCK.get(),
                    CAVE_GATE_BLOCK.get(),
                    SEWERAGE_GATE_BLOCK.get(),
                    DARK_GATE_BLOCK.get()
            ).build(null)); // The null argument is typically used for a datafixer, which isn't needed here for initial setup.


    /**
     * Registers all DeferredRegisters with the specified mod event bus.
     * This method should be called during mod initialization.
     *
     * @param eventBus The mod's event bus.
     */
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
        ChaoticDungeons.LOGGER.debug("ModRegisters: All deferred registers initialized and registered.");
    }

    /**
     * Helper method to create a BlockItem for a given block.
     * This is commonly used for blocks that should also exist as items in the inventory.
     *
     * @param name The registry name for the BlockItem.
     * @param block The RegistryObject of the block to create an item for.
     * @return A RegistryObject holding the newly registered BlockItem.
     */
    public static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}