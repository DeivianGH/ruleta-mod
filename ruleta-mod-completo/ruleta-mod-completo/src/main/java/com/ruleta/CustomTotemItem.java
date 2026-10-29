package com.ruleta;

import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

public final class CustomTotemItem extends Item {
    private final int variant;

    public CustomTotemItem(int variant) {
        super(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
        this.variant = variant;
    }

    public int variant() { return variant; }
}
