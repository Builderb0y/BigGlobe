package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.block.GrassBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(GrassBlock.class)
public class GrassBlock_UseCustomFeatureInBigGlobeWorlds {

	@Inject(method = "grow", at = @At("HEAD"), cancellable = true)
	private void bigglobe_useCustomFeatureInBigGlobeWorlds(ServerWorld world, Random random, BlockPos pos, BlockState state, CallbackInfo callback) {
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.grass_bonemeal_feature != null) {
			generator.grass_bonemeal_feature.value().generate(world, generator, world.random, pos);
			callback.cancel();
		}
	}
}