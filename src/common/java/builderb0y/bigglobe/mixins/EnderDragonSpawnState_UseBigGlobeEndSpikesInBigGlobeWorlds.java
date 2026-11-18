package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.features.EndSpikeRespawnFeature;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.configurations.EndSpikeConfiguration;
#endif

@Mixin(targets = "net/minecraft/world/level/dimension/end/DragonRespawnStage$3")
public class EnderDragonSpawnState_UseBigGlobeEndSpikesInBigGlobeWorlds {

	#if MC_VERSION >= MC_26_3_0

		/**
		normally I'd use ModifyExpressionValue or ModifyReceiver,
		but both of those require returning the same type as the original expression.
		in this case, EndSpikeFeature. but EndSpikeFeature became a record in MC 26.3,
		and therefore cannot be subclassed. so, I am wrapping the place() call instead.
		seems to be the least intrusive solution.
		*/
		@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/EndSpikeFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
		private boolean bigglobe_useCustomEndSpikeInBigGlobeWorlds(
			EndSpikeFeature instance,
			WorldGenLevel world,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BlockPos origin,
			Operation<Boolean> original
		) {
			if (chunkGenerator instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
				return new EndSpikeRespawnFeature(instance).place(world, chunkGenerator, random, origin);
			}
			else {
				return original.call(instance, world, chunkGenerator, random, origin);
			}
		}

	#else

		@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/feature/Feature;END_SPIKE:Lnet/minecraft/world/level/levelgen/feature/Feature;", opcode = Opcodes.GETSTATIC))
		private Feature<EndSpikeConfiguration> bigglobe_redirectSpikeFeature(Feature<EndSpikeConfiguration> oldValue, ServerLevel world) {
			if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
				return BigGlobeFeatures.END_SPIKE_RESPAWN;
			}
			else {
				return oldValue;
			}
		}

	#endif
}