package builderb0y.bigglobe.models;

import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.network.chat.Component;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeModels {

	public static void init() {
		CustomUnbakedBlockStateModel.register(BigGlobeMod.modID("ore"), OreUnbakedBlockStateModel.CODEC);
		ResourceLoader.registerBuiltinPack(
			BigGlobeMod.modID("mimic_ores"),
			FabricLoader.getInstance().getModContainer(BigGlobeMod.MODID).orElseThrow(),
			Component.translatable("resourcepack.bigglobe.mimic_ores"),
			PackActivationType.DEFAULT_ENABLED
		);
	}
}