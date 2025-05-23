// src/main/java/com/chaoticdungeons/chaoticdungeons/items/keys/DiamondKey.java
package com.chaoticdungeons.chaoticdungeons.items.keys;

import net.minecraft.world.item.Item;

/**
 * Represents the Diamond Key item.
 * This key is used to activate 'dark' type dungeons of difficulty 4.
 */
public class DiamondKey extends BaseKeyItem {
    public DiamondKey() {
        super(new Item.Properties().stacksTo(1), "dark", 4);
    }
}