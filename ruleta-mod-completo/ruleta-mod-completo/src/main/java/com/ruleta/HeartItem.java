package com.ruleta;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;

public final class HeartItem extends Item {
    public HeartItem() { super(new Item.Settings().maxCount(16)); }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) { return 32; }

    @Override
    public UseAction getUseAction(ItemStack stack) { return UseAction.EAT; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        player.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            float healing = (5 + world.random.nextInt(3)) * 2.0f;
            player.heal(healing);
            if (!player.getAbilities().creativeMode) stack.decrement(1);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.8f, 1.15f);
        }
        return stack;
    }
}
