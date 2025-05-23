// src/main/java/com/chaoticdungeons/chaoticdungeons/selectors/DungeonSelector.java
package com.chaoticdungeons.chaoticdungeons.selectors;

import com.chaoticdungeons.chaoticdungeons.ChaoticDungeons;
import com.chaoticdungeons.chaoticdungeons.dungeons.DungeonData;
import com.chaoticdungeons.chaoticdungeons.dungeons.DungeonRegistry;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for selecting suitable dungeons from the DungeonRegistry.
 * Provides methods to filter dungeons based on type and difficulty.
 */
public class DungeonSelector {

    /**
     * Selects a list of DungeonData objects that match the given type and minimum difficulty.
     *
     * @param type The desired dungeon type (e.g., "basic", "cave"). Case-insensitive.
     * @param minimumDifficulty The minimum difficulty required for the dungeon.
     * @return A list of DungeonData objects matching the criteria. Returns an empty list if none are found.
     */
    public List<DungeonData> selectDungeons(String type, int minimumDifficulty) {
        if (type == null || type.isEmpty()) {
            ChaoticDungeons.LOGGER.warn("DungeonSelector: Attempted to select dungeons with null or empty type.");
            return Collections.emptyList();
        }

        List<DungeonData> allDungeonsOfType = DungeonRegistry.getInstance().getAllDungeonsByType().get(type.toLowerCase());

        if (allDungeonsOfType == null || allDungeonsOfType.isEmpty()) {
            ChaoticDungeons.LOGGER.debug("DungeonSelector: No dungeons found for type '{}'.", type);
            return Collections.emptyList();
        }

        // Filter by difficulty
        List<DungeonData> filteredDungeons = allDungeonsOfType.stream()
                .filter(dungeon -> dungeon.difficulty() >= minimumDifficulty)
                .collect(Collectors.toList());

        if (filteredDungeons.isEmpty()) {
            ChaoticDungeons.LOGGER.debug("DungeonSelector: No dungeons found for type '{}' with minimum difficulty {}.", type, minimumDifficulty);
        } else {
            ChaoticDungeons.LOGGER.debug("DungeonSelector: Found {} dungeons for type '{}' with minimum difficulty {}.", filteredDungeons.size(), type, minimumDifficulty);
        }

        return filteredDungeons;
    }
}