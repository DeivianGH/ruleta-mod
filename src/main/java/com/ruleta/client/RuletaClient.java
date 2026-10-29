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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Reproduce la animacion de la ruleta (secuencia de PNG por color) para todos los jugadores. */
public class RuletaClient implements ClientModInitializer {
    public static final int FRAMES = 48;     // debe coincidir con tools/generar_frames.py
    private static final int TEX = 128;      // tamano del PNG
    private static final int DRAW = 112;     // tamano en pantalla
    private static final long SHOW_MS = 4000;

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
    private static final Identifier DEATH_FACE = Identifier.of(RuletaMod.ID, "textures/item/death_face.png");

    private static final int COLS = 8, ROWS = 6; // hoja de 8x6 cuadros por color
    private static final Identifier[] SHEETS = new Identifier[RuletaColor.values().length];

    static {
        for (RuletaColor c : RuletaColor.values())
            SHEETS[c.ordinal()] = Identifier.of(RuletaMod.ID, "textures/ruleta/" + c.id() + ".png");
    }

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

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Atmosphere.ID, (payload, ctx) -> deivilActive = payload.active());
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Death.ID, (payload, ctx) -> {
            deathName = payload.player();
            deathDuration = Math.max(20, payload.ticks()) * 50;
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
                float fade = Math.min(1f, Math.min(progress * 7f, (1f - progress) * 7f));
                int alpha = (int) (fade * 210);
                g.fill(w / 2 - 150, h / 2 - 47, w / 2 + 150, h / 2 + 48, (alpha << 24) | 0x100A0D);
                g.drawTexture(DEATH_FACE, w / 2 - 24, h / 2 - 38, 48, 48, 0, 0, 16, 16, 16, 16);
                int textAlpha = (int) (fade * 255);
                g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(deathName + " MURIÓ").styled(s -> s.withBold(true)), w / 2, h / 2 + 18, (textAlpha << 24) | 0xFF3030);
            }
        }

        if (startMs < 0) return;
        long el = now - startMs;
        if (el > spinMs + SHOW_MS) { startMs = -1; return; }

        int frame = el < spinMs ? (int) Math.min(FRAMES - 1, el * FRAMES / spinMs) : FRAMES - 1;
        if (frame != lastFrame) {
            lastFrame = frame;
            if (frame % 3 == 0 && frame < FRAMES - 1) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f));
            }
        }
        if (el >= spinMs && !endSoundPlayed) {
            endSoundPlayed = true;
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f));
        }

        int x = (w - DRAW) / 2;
        int y = h / 8;

        g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("RULETA").styled(s -> s.withBold(true)), w / 2, y - 12,
                deivilActive ? 0xFFFF3030 : 0xFFFFFFFF);
        g.drawTexture(SHEETS[finalIdx], x, y, DRAW, DRAW,
                (float) ((frame % COLS) * TEX), (float) ((frame / COLS) * TEX), TEX, TEX, COLS * TEX, ROWS * TEX);

        if (el >= spinMs) {
            RuletaColor rc = RuletaColor.byId(finalIdx);
            g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(rc.category).formatted(deivilActive ? net.minecraft.util.Formatting.RED : rc.fmt), w / 2, y + DRAW + 6, 0xFFFFFF);
            g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(finalName).formatted(deivilActive ? net.minecraft.util.Formatting.RED : net.minecraft.util.Formatting.WHITE), w / 2, y + DRAW + 20, 0xFFFFFF);
            if (!finalItemId.isBlank()) {
                var item = Registries.ITEM.get(Identifier.of(finalItemId));
                if (item != Items.AIR) g.drawItem(new ItemStack(item), w / 2 - 8, y + DRAW / 2 - 8);
            }
        }
    }
}
