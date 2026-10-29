package com.ruleta;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class Payloads {
    private Payloads() {}

    /** S2C: abre el menu del admin. */
    public record Open() implements CustomPayload {
        public static final Id<Open> ID = new Id<>(Identifier.of(RuletaMod.ID, "open"));
        public static final PacketCodec<ByteBuf, Open> CODEC = PacketCodec.unit(new Open());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** C2S: el admin pide lanzar la ruleta (color -1 = aleatorio, option -1 = aleatoria). */
    public record Request(int color, int option, String title, String custom) implements CustomPayload {
        public static final Id<Request> ID = new Id<>(Identifier.of(RuletaMod.ID, "request"));
        public static final PacketCodec<ByteBuf, Request> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, Request::color,
                PacketCodecs.INTEGER, Request::option,
                PacketCodecs.STRING, Request::title,
                PacketCodecs.STRING, Request::custom,
                Request::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** C2S: controls administrativos independientes del lanzamiento. */
    public record Control(int action, int color, String title, String text) implements CustomPayload {
        public static final Id<Control> ID = new Id<>(Identifier.of(RuletaMod.ID, "control"));
        public static final PacketCodec<ByteBuf, Control> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, Control::action,
                PacketCodecs.INTEGER, Control::color,
                PacketCodecs.STRING, Control::title,
                PacketCodecs.STRING, Control::text,
                Control::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: animacion de la ruleta para todos los jugadores. */
    public record Spin(int color, String name, int ticks, String itemId) implements CustomPayload {
        public static final Id<Spin> ID = new Id<>(Identifier.of(RuletaMod.ID, "spin"));
        public static final PacketCodec<ByteBuf, Spin> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, Spin::color,
                PacketCodecs.STRING, Spin::name,
                PacketCodecs.INTEGER, Spin::ticks,
                PacketCodecs.STRING, Spin::itemId,
                Spin::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: muestra unos segundos la escena de muerte cinematografica. */
    public record Death(String player, int ticks) implements CustomPayload {
        public static final Id<Death> ID = new Id<>(Identifier.of(RuletaMod.ID, "death"));
        public static final PacketCodec<ByteBuf, Death> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, Death::player,
                PacketCodecs.INTEGER, Death::ticks,
                Death::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record Atmosphere(boolean active) implements CustomPayload {
        public static final Id<Atmosphere> ID = new Id<>(Identifier.of(RuletaMod.ID, "atmosphere"));
        public static final PacketCodec<ByteBuf, Atmosphere> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOL, Atmosphere::active, Atmosphere::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
