package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.MiscConfiguredFeatures;
import net.minecraft.world.level.ServerWorldProperties;

import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;

@Mixin(MinecraftServer.class)
public class MinecraftServer_InitializeSpawnPoint {

	@Inject(method = "setupSpawn", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_setupSpawn(
		ServerWorld world,
		ServerWorldProperties worldProperties,
		boolean bonusChest,
		boolean debugWorld,
		CallbackInfo callback
	) {
		if (BigGlobeSpawnLocator.initWorldSpawn(world)) {
			if (bonusChest) {
				MiscConfiguredFeatures.BONUS_CHEST.value().generate(
					world,
					world.getChunkManager().getChunkGenerator(),
					world.random,
					new BlockPos(
						worldProperties.getSpawnX(),
						worldProperties.getSpawnY(),
						worldProperties.getSpawnZ()
					)
				);
			}
			callback.cancel();
		}
	}
}