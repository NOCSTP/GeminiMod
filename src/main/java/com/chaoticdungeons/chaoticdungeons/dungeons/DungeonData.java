// src/main/java/com/chaoticdungeons/chaoticdungeons/dungeons/DungeonData.java
package com.chaoticdungeons.chaoticdungeons.dungeons;

import com.google.gson.annotations.SerializedName;

/**
 * A record representing the data for a single dungeon, parsed from a JSON file.
 * Records are immutable data classes, ideal for holding configuration like this.
 *
 * @param structure The resource location path to the NBT structure file (e.g., "chaotic_dungeons:dungeons/basic_dungeon_1").
 * @param type The type of the dungeon (e.g., "basic", "cave", "sewerage", "dark").
 * @param difficulty The difficulty level of the dungeon (1-5).
 */
public record DungeonData(
        @SerializedName("structure") String structure,
        @SerializedName("type") String type,
        @SerializedName("difficulty") int difficulty) {

    /**
     * Validates if the dungeon type is one of the predefined valid types.
     * This is a utility method for internal use or simple checks.
     * More extensive validation is done in DungeonRegistry.
     *
     * @param type The type string to validate.
     * @return True if the type is valid, false otherwise.
     */
    public static boolean isValidType(String type) {
        return type != null && (type.equalsIgnoreCase("basic") ||
                type.equalsIgnoreCase("cave") ||
                type.equalsIgnoreCase("sewerage") ||
                type.equalsIgnoreCase("dark"));
    }
}