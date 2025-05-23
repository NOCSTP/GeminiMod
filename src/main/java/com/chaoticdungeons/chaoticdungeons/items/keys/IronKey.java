// src/main/java/com/chaoticdungeons/chaoticdungeons/items/keys/IronKey.java
package com.chaoticdungeons.chaoticdungeons.items.keys;

import net.minecraft.world.item.Item;

/**
 * Represents the Iron Key item.
 * This key is used to activate 'cave' type dungeons of difficulty 2.
 */
public class IronKey extends BaseKeyItem {
    public IronKey() {
        super(new Item.Properties().stacksTo(1), "cave", 2);
    }
}