package com.ruleta;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class HeartItem extends Item {
    public HeartItem() { super(new Item.Settings().maxCount(16)); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            float healing = (5 + world.random.nextInt(3)) * 2.0f;
            player.heal(healing);
            if (!player.getAbilities().creativeMode) stack.decrement(1);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.8f, 1.15f);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
