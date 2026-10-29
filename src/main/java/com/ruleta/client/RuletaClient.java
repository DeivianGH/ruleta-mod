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
    private static int lastFrame = -1;
    private static boolean endSoundPlayed;

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
            spinMs = payload.ticks() * 50L;
            startMs = System.currentTimeMillis();
            lastFrame = -1;
            endSoundPlayed = false;
        });

        HudRenderCallback.EVENT.register((g, tickCounter) -> render(g));
    }

    private static void render(DrawContext g) {
        if (startMs < 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        long el = System.currentTimeMillis() - startMs;
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

        int w = g.getScaledWindowWidth();
        int h = g.getScaledWindowHeight();
        int x = (w - DRAW) / 2;
        int y = h / 8;

        g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("RULETA"), w / 2, y - 12, 0xFFFFFF);
        g.drawTexture(SHEETS[finalIdx], x, y, DRAW, DRAW,
                (float) ((frame % COLS) * TEX), (float) ((frame / COLS) * TEX), TEX, TEX, COLS * TEX, ROWS * TEX);

        if (el >= spinMs) {
            RuletaColor rc = RuletaColor.byId(finalIdx);
            g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(rc.category).formatted(rc.fmt), w / 2, y + DRAW + 6, 0xFFFFFF);
            g.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(finalName), w / 2, y + DRAW + 20, 0xFFFFFF);
        }
    }
}
