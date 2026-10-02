package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.level.chunk.status.ChunkPyramid;

import builderb0y.bigglobe.config.BigGlobeMixinConfig;

@Mixin(ChunkPyramid.class)
public class ChunkPyramid_ConfigurableFeatureRadius {

	@ModifyConstant(method = "lambda$static$7", constant = @Constant(intValue = 1), expect = 2)
	private static int bigglobe_modifyFeatureChunkRadius(int original) {
		return Math.max(original, Math.min(BigGlobeMixinConfig.INSTANCE.featureChunkRadius, 8));
	}

	/**
	I still don't 100% understand why this one is necessary too, but ishland recommended it.
	without it, vanilla will sometimes cut features off at chunk borders,
	and C2ME sometimes modifies LevelChunk's from the wrong thread.
	this modification fixes both issues, but makes worldgen
	even slower than with just the above method alone.
	*/
	@ModifyConstant(method = "lambda$static$9", constant = @Constant(intValue = 1), expect = 2)
	private static int bigglobe_modifyLightRadiusToFixBuggyVanillaBehavior(int original) {
		return Math.max(original, Math.min(BigGlobeMixinConfig.INSTANCE.featureChunkRadius, 8));
	}
}