package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.MiscConfiguredFeatures;
import net.minecraft.world.level.ServerWorldProperties;

import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.WorldPropertiesVersions;

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
				#if MC_VERSION <= MC_1_19_2
					ConfiguredFeature<?, ?> feature = MiscConfiguredFeatures.BONUS_CHEST.value();
				#else
					ConfiguredFeature<?, ?> feature = world.getRegistryManager().get(RegistryKeyVersions.configuredFeature()).get(MiscConfiguredFeatures.BONUS_CHEST);
				#endif
				if (feature != null) feature.generate(
					world,
					world.getChunkManager().getChunkGenerator(),
					world.random,
					WorldPropertiesVersions.getSpawnPos(worldProperties)
				);
			}
			callback.cancel();
		}
	}
}