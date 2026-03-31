package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(GrassBlock.class)
public class GrassBlock_UseCustomFeatureInBigGlobeWorlds {

	@Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
	private void bigglobe_useCustomFeatureInBigGlobeWorlds(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, CallbackInfo callback) {
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.grass_bonemeal_feature != null) {
			generator.grass_bonemeal_feature.value().place(world, generator, world.getRandom(), pos);
			callback.cancel();
		}
	}
}