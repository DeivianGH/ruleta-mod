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
    public record Request(int color, int option, String custom) implements CustomPayload {
        public static final Id<Request> ID = new Id<>(Identifier.of(RuletaMod.ID, "request"));
        public static final PacketCodec<ByteBuf, Request> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, Request::color,
                PacketCodecs.INTEGER, Request::option,
                PacketCodecs.STRING, Request::custom,
                Request::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: animacion de la ruleta para todos los jugadores. */
    public record Spin(int color, String name, int ticks) implements CustomPayload {
        public static final Id<Spin> ID = new Id<>(Identifier.of(RuletaMod.ID, "spin"));
        public static final PacketCodec<ByteBuf, Spin> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, Spin::color,
                PacketCodecs.STRING, Spin::name,
                PacketCodecs.INTEGER, Spin::ticks,
                Spin::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
