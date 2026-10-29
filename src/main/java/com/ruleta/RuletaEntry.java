package com.ruleta;

import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;

public record RuletaEntry(RuletaColor color, String name, String chatText, int durationTicks,
                          Consumer<MinecraftServer> start, Consumer<MinecraftServer> stop) {}
