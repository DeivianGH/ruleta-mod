package com.ruleta;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class AdminWandItem extends Item {
    public AdminWandItem() {
        super(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            if (sp.hasPermissionLevel(2)) {
                ServerPlayNetworking.send(sp, new Payloads.Open());
            } else {
                sp.sendMessage(Text.literal("Solo un admin puede usar esto.").formatted(Formatting.RED), true);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
