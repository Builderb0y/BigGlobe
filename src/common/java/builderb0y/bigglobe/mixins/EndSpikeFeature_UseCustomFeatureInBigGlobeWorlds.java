package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.level.levelgen.feature.configurations.EndSpikeConfiguration;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.features.EndSpikeFeatureReplacement;

@Mixin(EndSpikeFeature.class)
public class EndSpikeFeature_UseCustomFeatureInBigGlobeWorlds {

	@Inject(method = "placeSpike", at = @At("HEAD"), cancellable = true)
	private void bigglobe_placeSpike(
		ServerLevelAccessor world,
		RandomSource random,
		EndSpikeConfiguration config,
		EndSpike spike,
		CallbackInfo callback
	) {
		if (
			world instanceof WorldGenLevel castWorld &&
			world.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.game_mechanics != null &&
			generator.game_mechanics.end() != null &&
			EndSpikeFeatureReplacement.place(castWorld, generator, spike, config.getCrystalBeamTarget(), config.isCrystalInvulnerable())
		) {
			callback.cancel();
		}
	}
}