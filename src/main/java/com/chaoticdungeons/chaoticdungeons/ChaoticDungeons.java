// src/main/java/com/chaoticdungeons/chaoticdungeons/ChaoticDungeons.java
package com.chaoticdungeons.chaoticdungeons;

import com.chaoticdungeons.chaoticdungeons.dungeons.DungeonRegistry; // Keep the import, but we won't call loadDungeonData directly
import com.chaoticdungeons.chaoticdungeons.registration.ModRegisters;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * The main class for the Chaotic Dungeons mod.
 * This class handles mod initialization, registers event listeners, and sets up common mod functionalities.
 */
@Mod(ChaoticDungeons.MOD_ID)
public class ChaoticDungeons {
    /**
     * The unique identifier for this mod.
     */
    public static final String MOD_ID = "chaotic_dungeons";
    /**
     * The logger instance for the Chaotic Dungeons mod, used for all logging operations.
     */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Constructor for the main mod class.
     * Registers the mod's event bus listeners and deferred registers.
     */
    public ChaoticDungeons() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the common setup event listener
        modEventBus.addListener(this::commonSetup);

        // Register all deferred registers for blocks, items, and block entities
        ModRegisters.register(modEventBus);
    }

    /**
     * This method is called during the common setup phase of mod initialization.
     * It's used for tasks that need to be run on both client and server, like loading data.
     *
     * @param event The FMLCommonSetupEvent instance.
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Chaotic Dungeons: Common setup started.");
        // The DungeonRegistry handles its own data loading via the resource reload system.
        // No need to manually call loadDungeonData() here.
        // It's already registered as a reload listener via AddReloadListenerEvent.
        LOGGER.info("Chaotic Dungeons: Common setup finished.");
    }
}