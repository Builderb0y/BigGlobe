package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.level.LevelAccessor;

import builderb0y.bigglobe.blockdefs.BigGlobeBlockTags;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(PolarBear.class)
public abstract class PolarBear_MakeSpawnableOnSnow extends Animal {

	public PolarBear_MakeSpawnableOnSnow() {
		super(null, null);
	}

	@Inject(method = "checkPolarBearSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_makeSpawnableOnSnow(
		EntityType<PolarBear> type,
		LevelAccessor level,
		EntitySpawnReason spawnReason,
		BlockPos pos,
		RandomSource random,
		CallbackInfoReturnable<Boolean> callback
	) {
		if (level.getChunkSource() instanceof ServerChunkCache cache && cache.getGenerator() instanceof BigGlobeScriptedChunkGenerator) {
			callback.setReturnValue(isBrightEnoughToSpawn(level, pos) && level.getBlockState(pos.below()).is(BigGlobeBlockTags.POLAR_BEARS_SPAWNABLE_ON));
		}
	}
}