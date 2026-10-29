package com.ruleta.mixin;

import com.ruleta.Catalog;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Con "Fuego eterno" activo, el fuego del Nether no se consume ni se propaga hasta que lo apague agua. */
@Mixin(FireBlock.class)
public class FireBlockMixin {
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void ruleta$eternal(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (Catalog.FLAGS.contains("eternalfire") && world.getRegistryKey() == World.NETHER) {
            // se reprograma para que, al terminar el evento, el fuego vuelva a comportarse normal
            world.scheduleBlockTick(pos, (FireBlock) (Object) this, 30 + random.nextInt(10));
            ci.cancel();
        }
    }
}
