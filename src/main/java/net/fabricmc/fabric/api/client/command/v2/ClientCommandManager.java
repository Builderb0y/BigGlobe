package net.fabricmc.fabric.api.client.command.v2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class ClientCommandManager {

	public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}

	public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
	}
}
