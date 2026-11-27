package builderb0y.bigglobe.mixins;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.features.EndSpikeFeatureReplacement;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.configurations.EndSpikeConfiguration;
#endif

@Mixin(EndSpikeFeature.class)
public abstract class EndSpikeFeature_UseCustomFeatureInBigGlobeWorlds {

	@Shadow public abstract Optional<BlockPos> crystalBeamTarget();

	@Shadow public abstract boolean crystalInvulnerable();

	@Inject(method = "placeSpike", at = @At("HEAD"), cancellable = true)
	private void bigglobe_placeSpike(
		ServerLevelAccessor world,
		RandomSource random,
		#if MC_VERSION < MC_26_3_0
			EndSpikeConfiguration config,
		#endif
		EndSpike spike,
		CallbackInfo callback
	) {
		if (
			world instanceof WorldGenLevel castWorld &&
			world.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.game_mechanics != null &&
			generator.game_mechanics.end() != null &&
			EndSpikeFeatureReplacement.place(
				castWorld,
				generator,
				spike,
				#if MC_VERSION >= MC_26_3_0
					this.crystalBeamTarget().orElse(null),
					this.crystalInvulnerable()
				#else
					config.getCrystalBeamTarget(),
					config.isCrystalInvulnerable()
				#endif
			)
		) {
			callback.cancel();
		}
	}
}