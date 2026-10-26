package builderb0y.bigglobe.fluids;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeFluids {

	public static final FlowingFluid
		SOUL_LAVA = register("soul_lava", new SoulLavaFluid.Still()),
		FLOWING_SOUL_LAVA = register("flowing_soul_lava", new SoulLavaFluid.Flowing());

	public static void init() {
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void initClient() {
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
		return Registry.register(BuiltInRegistries.FLUID, BigGlobeMod.modID(name), fluid);
	}
}