package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;

import builderb0y.bigglobe.compat.dhChunkGen.DhScriptedWorldGenerator;

@Mixin(DhScriptedWorldGenerator.class)
public class DhScriptedWorldGenerator_BackwardsCompatibility {

	//actual backwards compatibility is done via ASM instead.
	//just need a marker mixin for that.
}