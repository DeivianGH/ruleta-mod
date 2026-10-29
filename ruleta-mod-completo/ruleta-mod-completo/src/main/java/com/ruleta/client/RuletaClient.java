package com.ruleta.client;

import com.ruleta.Payloads;
import com.ruleta.RuletaColor;
import com.ruleta.RuletaMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Reproduce los atlas transparentes de la ruleta y la animacion cinematografica. */
public class RuletaClient implements ClientModInitializer {
    private static final int WHEEL_FRAMES = 40;
    private static final int WHEEL_TEX = 256;
    private static final int WHEEL_COLS = 8, WHEEL_ROWS = 5;
    private static final int DEATH_FRAME_W = 160, DEATH_FRAME_H = 249;
    private static final int DEATH_COLS = 16, DEATH_ROWS = 12, DEATH_FRAMES = 174;
    private static final long SHOW_MS = 2500;

    private static long startMs = -1;
    private static long spinMs;
    private static int finalIdx;
    private static String finalName = "";
    private static String finalItemId = "";
    private static int lastFrame = -1;
    private static boolean endSoundPlayed;
    private static boolean deivilActive;
    private static long deathStarted = -1;
    private static int deathDuration = 4000;
    private static String deathName = "";
    private static final Identifier WHEEL = Identifier.of(RuletaMod.ID, "textures/animation/ruleta_wheel.png");
    private static final Identifier DEATH = Identifier.of(RuletaMod.ID, "textures/animation/muerte.png");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Open.ID, (payload, ctx) ->
                ctx.client().setScreen(new RuletaScreen()));

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Spin.ID, (payload, ctx) -> {
            finalIdx = payload.color();
            finalName = payload.name();
            finalItemId = payload.itemId();
            spinMs = payload.ticks() * 50L;
            startMs = System.currentTimeMillis();
            lastFrame = -1;
            endSoundPlayed = false;
        });

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Atmosphere.ID, (payload, ctx) -> {
            deivilActive = payload.active();
            if (payload.active()) mc().getSoundManager().play(PositionedSoundInstance.music(RuletaMod.DEIVIL_MUSIC));
            else mc().getSoundManager().stopSounds(Identifier.of(RuletaMod.ID, "momento_deivil"), SoundCategory.MUSIC);
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Death.ID, (payload, ctx) -> {
            deathName = payload.player();
            deathDuration = Math.min(180, Math.max(20, payload.ticks())) * 50;
            deathStarted = System.currentTimeMillis();
        });

        HudRenderCallback.EVENT.register((g, tickCounter) -> render(g));
    }

    private static void render(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        int w = g.getScaledWindowWidth();
        int h = g.getScaledWindowHeight();
        if (deivilActive) g.fill(0, 0, w, h, 0x2F9B1018);

        if (deathStarted >= 0) {
            long elapsedDeath = now - deathStarted;
            if (elapsedDeath >= deathDuration) deathStarted = -1;
            else {
                float progress = elapsedDeath / (float) deathDuration;
                float fade = Math.min(1f, Math.min(progress * 5f, (1f - progress) * 5f));
                int frame = Math.min(DEATH_FRAMES - 1, (int) (progress * DEATH_FRAMES));
                float scale = Math.min((w * 0.48f) / DEATH_FRAME_W, (h * 0.68f) / DEATH_FRAME_H);
                int dw = Math.max(1, (int) (DEATH_FRAME_W * scale));
                int dh = Math.max(1, (int) (DEATH_FRAME_H * scale));
                int dx = (w - dw) / 2, dy = (h - dh) / 2 - (deathName.isBlank() ? 0 : 12);
                g.setShaderColor(1f, 1f, 1f, fade);
                g.drawTexture(DEATH, dx, dy, dw, dh,
                        (frame % DEATH_COLS) * DEATH_FRAME_W, (frame / DEATH_COLS) * DEATH_FRAME_H,
                        DEATH_FRAME_W, DEATH_FRAME_H, DEATH_COLS * DEATH_FRAME_W, DEATH_ROWS * DEATH_FRAME_H);
                g.setShaderColor(1f, 1f, 1f, 1f);
                if (!deathName.isBlank()) {
                    int textAlpha = (int) (fade * 255);
                    g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(deathName + " MURIÓ").styled(s -> s.withBold(true)),
                            w / 2, dy + dh + 3, (textAlpha << 24) | 0xFFFF3030);
                }
            }
        }

        if (startMs < 0) return;
        long el = now - startMs;
        if (el > spinMs + SHOW_MS) { startMs = -1; return; }

        int frame = el < spinMs ? (int) Math.min(WHEEL_FRAMES - 1, el * WHEEL_FRAMES / spinMs) : WHEEL_FRAMES - 1;
        if (frame != lastFrame) {
            lastFrame = frame;
            if (frame % 3 == 0 && frame < WHEEL_FRAMES - 1) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f));
            }
        }
        if (el >= spinMs && !endSoundPlayed) {
            endSoundPlayed = true;
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f));
        }

        int draw = Math.min(256, Math.min(w - 32, h / 2));
        int x = (w - draw) / 2;
        int y = h / 8;

        g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("RULETA").styled(s -> s.withBold(true)), w / 2, y - 12,
                deivilActive ? 0xFFFF3030 : 0xFFFFFFFF);
        g.drawTexture(WHEEL, x, y, draw, draw,
                (frame % WHEEL_COLS) * WHEEL_TEX, (frame / WHEEL_COLS) * WHEEL_TEX,
                WHEEL_TEX, WHEEL_TEX, WHEEL_COLS * WHEEL_TEX, WHEEL_ROWS * WHEEL_TEX);

        if (el >= spinMs) {
            RuletaColor rc = RuletaColor.byId(finalIdx);
            g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(rc.category).formatted(deivilActive ? net.minecraft.util.Formatting.RED : rc.fmt), w / 2, y + draw + 6, 0xFFFFFF);
            g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(finalName).formatted(deivilActive ? net.minecraft.util.Formatting.RED : net.minecraft.util.Formatting.WHITE), w / 2, y + draw + 20, 0xFFFFFF);
            if (!finalItemId.isBlank()) g.drawItem(Registries.ITEM.get(Identifier.of(finalItemId)).getDefaultStack(), w / 2 - 8, y + draw / 2 - 8);
        }
    }

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
}
