package net.fabricmc.fabric.api.command.v2;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.resources.ResourceLocation;

public class ArgumentTypeRegistry {

	public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void registerArgumentType(
		ResourceLocation id,
		Class<A> argumentClass,
		ArgumentTypeInfo<A, T> info
	) {
		ArgumentTypeInfos.registerByClass(argumentClass, info);
	}
}
