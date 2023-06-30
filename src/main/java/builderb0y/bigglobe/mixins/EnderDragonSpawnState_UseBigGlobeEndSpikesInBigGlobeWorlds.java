package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.bigglobe.chunkgen.BigGlobeEndChunkGenerator;
import builderb0y.bigglobe.features.BigGlobeFeatures;

@Mixin(targets = "net/minecraft/entity/boss/dragon/EnderDragonSpawnState$3")
public class EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds {

	@Redirect(method = "run", at = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/feature/Feature;END_SPIKE:Lnet/minecraft/world/gen/feature/Feature;"))
	private Feature<EndSpikeFeatureConfig> bigglobe_redirectSpikeFeature(ServerWorld world) {
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeEndChunkGenerator) {
			return BigGlobeFeatures.END_SPIKE;
		}
		else {
			return Feature.END_SPIKE;
		}
	}
}