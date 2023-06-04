package builderb0y.bigglobe.fluids;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;

import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeFluids {

	public static final FlowableFluid
		SOUL_LAVA         = register("soul_lava",         new SoulLavaFluid.Still()),
		FLOWING_SOUL_LAVA = register("flowing_soul_lava", new SoulLavaFluid.Flowing());

	public static void init() {}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		/*
		ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).register((atlasTexture, registry) -> {
			registry.register(BigGlobeMod.modID("block/soul_lava_still"));
			registry.register(BigGlobeMod.modID("block/soul_lava_flowing"));
		});
		*/
		FluidRenderHandlerRegistry.INSTANCE.register(
			SOUL_LAVA,
			FLOWING_SOUL_LAVA,
			new SimpleFluidRenderHandler(
				BigGlobeMod.modID("block/soul_lava_still"),
				BigGlobeMod.modID("block/soul_lava_flowing")
			)
		);
	}

	public static <F extends Fluid> F register(String name, F fluid) {
		return Registry.register(Registry.FLUID, BigGlobeMod.modID(name), fluid);
	}
}