package com.ruleta.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void ruleta$slightlyReduceChat(DrawContext context, int currentTick, int mouseX, int mouseY,
                                           boolean focused, CallbackInfo ci) {
        context.getMatrices().push();
        float scale = 0.96f;
        float height = MinecraftClient.getInstance().getWindow().getScaledHeight();
        context.getMatrices().translate(0.0, height * (1.0 - scale), 0.0);
        context.getMatrices().scale(scale, scale, 1.0f);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ruleta$restoreChatScale(DrawContext context, int currentTick, int mouseX, int mouseY,
                                         boolean focused, CallbackInfo ci) {
        context.getMatrices().pop();
    }
}
