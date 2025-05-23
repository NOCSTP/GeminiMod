// src/main/java/com/chaoticdungeons/chaoticdungeons/items/keys/BaseKeyItem.java
package com.chaoticdungeons.chaoticdungeons.items.keys;

import net.minecraft.world.item.Item;

/**
 * Base class for all key items in Chaotic Dungeons.
 * Provides a common interface for key-related properties, like the dungeon type it can open.
 */
public abstract class BaseKeyItem extends Item {
    private final String opensDungeonType;
    private final int opensDungeonDifficulty;

    /**
     * Constructor for a BaseKeyItem.
     *
     * @param properties The item properties.
     * @param opensDungeonType The type of dungeon this key can open (e.g., "basic", "cave").
     * @param opensDungeonDifficulty The minimum difficulty of dungeon this key can open.
     */
    public BaseKeyItem(Properties properties, String opensDungeonType, int opensDungeonDifficulty) {
        super(properties);
        this.opensDungeonType = opensDungeonType;
        this.opensDungeonDifficulty = opensDungeonDifficulty;
    }

    /**
     * Gets the type of dungeon this key is designed to open.
     *
     * @return The dungeon type string.
     */
    public String getOpensDungeonType() {
        return opensDungeonType;
    }

    /**
     * Gets the minimum difficulty of dungeon this key can open.
     *
     * @return The minimum dungeon difficulty.
     */
    public int getOpensDungeonDifficulty() {
        return opensDungeonDifficulty;
    }
}