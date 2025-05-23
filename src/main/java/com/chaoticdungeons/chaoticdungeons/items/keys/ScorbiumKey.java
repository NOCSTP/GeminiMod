// src/main/java/com/chaoticdungeons/chaoticdungeons/items/keys/ScorbiumKey.java
package com.chaoticdungeons.chaoticdungeons.items.keys;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Represents the Scorbium Key item.
 * This is a high-tier key, used to activate 'dark' type dungeons of difficulty 5.
 */
public class ScorbiumKey extends BaseKeyItem {
    public ScorbiumKey() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), "dark", 5);
    }
}