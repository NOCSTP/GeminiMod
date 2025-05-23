// src/main/java/com/chaoticdungeons/chaoticdungeons/items/keys/GoldenKey.java
package com.chaoticdungeons.chaoticdungeons.items.keys;

import net.minecraft.world.item.Item;

/**
 * Represents the Golden Key item.
 * This key is used to activate 'sewerage' type dungeons of difficulty 3.
 */
public class GoldenKey extends BaseKeyItem {
    public GoldenKey() {
        super(new Item.Properties().stacksTo(1), "sewerage", 3);
    }
}