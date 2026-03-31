package builderb0y.bigglobe.fluids;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeFluids {

	public static final FlowingFluid
		SOUL_LAVA = register("soul_lava", new SoulLavaFluid.Still()),
		FLOWING_SOUL_LAVA = register("flowing_soul_lava", new SoulLavaFluid.Flowing());

	public static void init() {}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		FluidRenderingRegistry.register(
			SOUL_LAVA,
			FLOWING_SOUL_LAVA,
			new FluidModel.Unbaked(
				new Material(BigGlobeMod.modID("block/soul_lava_still")),
				new Material(BigGlobeMod.modID("block/soul_lava_flowing")),
				null,
				null
			)
		);
	}

	public static <F extends Fluid> F register(String name, F fluid) {
		return Registry.register(BuiltInRegistries.FLUID, BigGlobeMod.modID(name), fluid);
	}
}