// src/main/java/com/chaoticdungeons/chaoticdungeons/items/keys/BronzeKey.java
package com.chaoticdungeons.chaoticdungeons.items.keys;

import net.minecraft.world.item.Item;

/**
 * Represents the Bronze Key item.
 * This key is used to activate 'basic' type dungeons of difficulty 1.
 */
public class BronzeKey extends BaseKeyItem {
    public BronzeKey() {
        super(new Item.Properties().stacksTo(1), "basic", 1);
    }
}