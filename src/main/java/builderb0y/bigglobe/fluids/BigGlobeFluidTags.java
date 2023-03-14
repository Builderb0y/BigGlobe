package builderb0y.bigglobe.fluids;

import net.minecraft.fluid.Fluid;
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeFluidTags {

	public static final TagKey<Fluid> SOUL_LAVA = TagKey.of(Registry.FLUID_KEY, BigGlobeMod.modID("soul_lava"));
}