package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndSpikeConfiguration;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.features.BigGlobeFeatures;

@Mixin(targets = "net/minecraft/world/level/dimension/end/DragonRespawnStage$3")
public class EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds {

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/feature/Feature;END_SPIKE:Lnet/minecraft/world/level/levelgen/feature/Feature;", opcode = Opcodes.GETSTATIC))
	private Feature<EndSpikeConfiguration> bigglobe_redirectSpikeFeature(Feature<EndSpikeConfiguration> oldValue, ServerLevel world) {
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
			return BigGlobeFeatures.END_SPIKE_RESPAWN;
		}
		else {
			return oldValue;
		}
	}
}