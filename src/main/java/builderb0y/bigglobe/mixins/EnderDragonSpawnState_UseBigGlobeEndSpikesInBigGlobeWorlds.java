package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.features.BigGlobeFeatures;

@Mixin(targets = "net/minecraft/entity/boss/dragon/EnderDragonSpawnState$3")
public class EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds {

	//todo: replace with ModifyExpressionValue.
	@Redirect(method = "run", at = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/feature/Feature;END_SPIKE:Lnet/minecraft/world/gen/feature/Feature;"))
	private Feature<EndSpikeFeatureConfig> bigglobe_redirectSpikeFeature(ServerWorld world) {
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			return BigGlobeFeatures.END_SPIKE_RESPAWN;
		}
		else {
			return Feature.END_SPIKE;
		}
	}
}