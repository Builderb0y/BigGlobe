package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.features.BigGlobeFeatures;

@Mixin(targets = "net/minecraft/world/level/dimension/end/DragonRespawnAnimation$3")
public class EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds {

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/feature/Feature;END_SPIKE:Lnet/minecraft/world/level/levelgen/feature/Feature;", opcode = Opcodes.GETSTATIC))
	private Feature<SpikeConfiguration> bigglobe_redirectSpikeFeature(Feature<SpikeConfiguration> oldValue, ServerLevel world) {
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			return BigGlobeFeatures.END_SPIKE_RESPAWN;
		}
		else {
			return oldValue;
		}
	}
}