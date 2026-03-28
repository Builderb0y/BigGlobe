package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

@Mixin(LevelStem.class)
public class DimensionOptions_CheckHeights {

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_validate(Holder<DimensionType> dimensionTypeEntry, ChunkGenerator chunkGenerator, CallbackInfo ci) {
		if (BigGlobeConfig.INSTANCE.get().checkWorldHeight && chunkGenerator instanceof BigGlobeScriptedChunkGenerator) {
			DimensionType world = dimensionTypeEntry.value();
			if (world.minY() != chunkGenerator.getMinY() || world.height() != chunkGenerator.getGenDepth()) {
				throw new IllegalArgumentException(
					"[Big Globe]: Dimension type height ("
					+ world.minY()
					+ " to "
					+ (world.minY() + world.height())
					+ ") does not match chunk generator height ("
					+ chunkGenerator.getMinY()
					+ " to "
					+ (chunkGenerator.getMinY() + chunkGenerator.getGenDepth())
					+ "). If this discrepancy is intentional, you can disable this check in Big Globe's config file."
				);
			}
		}
	}
}