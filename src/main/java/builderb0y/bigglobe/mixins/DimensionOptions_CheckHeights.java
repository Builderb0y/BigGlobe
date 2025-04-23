package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;

@Mixin(DimensionOptions.class)
public class DimensionOptions_CheckHeights {

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_validate(RegistryEntry<DimensionType> dimensionTypeEntry, ChunkGenerator chunkGenerator, CallbackInfo ci) {
		if (BigGlobeConfig.INSTANCE.get().checkWorldHeight && chunkGenerator instanceof BigGlobeScriptedChunkGenerator) {
			DimensionType world = dimensionTypeEntry.value();
			if (world.minY() != chunkGenerator.getMinimumY() || world.height() != chunkGenerator.getWorldHeight()) {
				throw new IllegalArgumentException(
					"[Big Globe]: Dimension type height ("
					+ world.minY()
					+ " to "
					+ (world.minY() + world.height())
					+ ") does not match chunk generator height ("
					+ chunkGenerator.getMinimumY()
					+ " to "
					+ (chunkGenerator.getMinimumY() + chunkGenerator.getWorldHeight())
					+ "). If this discrepancy is intentional, you can disable this check in Big Globe's config file."
				);
			}
		}
	}
}