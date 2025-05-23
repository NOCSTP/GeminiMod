// src/main/java/com/chaoticdungeons/chaoticdungeons/dungeons/DungeonRegistry.java
package com.chaoticdungeons.chaoticdungeons.dungeons;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Singleton registry for managing and loading dungeon data from JSON files.
 * This class handles reading JSON files from the mod's data directory, parsing them into DungeonData objects,
 * and performing validation on the parsed data. It integrates with Minecraft's resource reload system.
 */
public class DungeonRegistry extends SimplePreparableReloadListener<List<DungeonData>> {
    private static final String DUNGEON_DATA_PATH = "dungeons";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static DungeonRegistry INSTANCE;

    // Stores dungeon data, organized by dungeon type for efficient lookup.
    private final Map<String, List<DungeonData>> registeredDungeons = new HashMap<>();

    /**
     * Private constructor to enforce the singleton pattern.
     * Registers this instance as a reload listener for resource packs.
     */
    private DungeonRegistry() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Returns the singleton instance of the DungeonRegistry.
     *
     * @return The singleton DungeonRegistry instance.
     */
    public static DungeonRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DungeonRegistry();
        }
        return INSTANCE;
    }

    /**
     * Event listener for adding reload listeners.
     * Ensures the DungeonRegistry is registered with the Minecraft resource manager.
     *
     * @param event The AddReloadListenerEvent.
     */
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(getInstance());
        ChaoticDungeons.LOGGER.debug("DungeonRegistry registered as a reload listener.");
    }

    /**
     * Prepares data by reading all dungeon JSON files. This method is called asynchronously.
     *
     * @param resourceManager The resource manager to access mod resources.
     * @param profiler The profiler for performance monitoring.
     * @return A CompletableFuture containing a list of parsed DungeonData objects.
     */
    @Override
    protected List<DungeonData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.startTick();
        List<DungeonData> loadedDungeons = new ArrayList<>();
        ChaoticDungeons.LOGGER.info("DungeonRegistry: Loading dungeon data from resources...");

        // Get all resources in the 'data/chaotic_dungeons/dungeons' directory with a '.json' extension
        resourceManager.listResources(String.valueOf(new ResourceLocation(ChaoticDungeons.MOD_ID, DUNGEON_DATA_PATH)), (path) -> path.getPath().endsWith(".json"))
                .forEach((resourceLocation, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        DungeonData data = GSON.fromJson(reader, DungeonData.class);
                        if (data != null && validateDungeonData(data, resourceLocation.getPath())) {
                            loadedDungeons.add(data);
                            ChaoticDungeons.LOGGER.debug("Successfully loaded dungeon data: {}", resourceLocation.getPath());
                        } else {
                            ChaoticDungeons.LOGGER.warn("Skipping invalid dungeon data from file: {}", resourceLocation.getPath());
                        }
                    } catch (JsonSyntaxException e) {
                        ChaoticDungeons.LOGGER.error("JSON Syntax Error in dungeon data file {}: {}", resourceLocation.getPath(), e.getMessage());
                    } catch (Exception e) {
                        ChaoticDungeons.LOGGER.error("Failed to read dungeon data from file {}: {}", resourceLocation.getPath(), e.getMessage());
                    }
                });
        profiler.endTick();
        return loadedDungeons;
    }

    /**
     * Applies the prepared data to the mod's active data structures. This method is called on the main thread.
     *
     * @param p_215312_1_ The prepared list of DungeonData.
     * @param resourceManager The resource manager.
     * @param profiler The profiler.
     */
    @Override
    protected void apply(List<DungeonData> p_215312_1_, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.startTick();
        registeredDungeons.clear(); // Clear existing data to prepare for new load

        p_215312_1_.forEach(data -> {
            // Group dungeons by type for quick lookup
            registeredDungeons.computeIfAbsent(data.type(), k -> new ArrayList<>()).add(data);
        });

        ChaoticDungeons.LOGGER.info("DungeonRegistry: Loaded {} total valid dungeon entries.",
                registeredDungeons.values().stream().mapToInt(List::size).sum());
        profiler.endTick();
    }

    /**
     * Validates a single DungeonData object against predefined rules.
     *
     * @param data The DungeonData object to validate.
     * @param fileName The name of the file from which the data was loaded, for logging purposes.
     * @return True if the data is valid, false otherwise.
     */
    private boolean validateDungeonData(DungeonData data, String fileName) {
        if (data.structure() == null || data.structure().isEmpty()) {
            ChaoticDungeons.LOGGER.error("Dungeon data from '{}' is missing 'structure' field.", fileName);
            return false;
        }
        if (data.type() == null || data.type().isEmpty()) {
            ChaoticDungeons.LOGGER.error("Dungeon data from '{}' is missing 'type' field.", fileName);
            return false;
        }
        if (data.difficulty() < 1 || data.difficulty() > 5) {
            ChaoticDungeons.LOGGER.error("Dungeon data from '{}' has invalid 'difficulty' (must be 1-5): {}", fileName, data.difficulty());
            return false;
        }

        // Validate type enum
        List<String> validTypes = List.of("basic", "cave", "sewerage", "dark");
        if (!validTypes.contains(data.type().toLowerCase())) {
            ChaoticDungeons.LOGGER.error("Dungeon data from '{}' has invalid 'type' (must be one of {}): {}", fileName, validTypes, data.type());
            return false;
        }
        return true;
    }

    /**
     * Returns an unmodifiable map of all registered dungeons, grouped by type.
     *
     * @return A map where keys are dungeon types and values are lists of DungeonData.
     */
    public Map<String, List<DungeonData>> getAllDungeonsByType() {
        return Collections.unmodifiableMap(registeredDungeons);
    }
}