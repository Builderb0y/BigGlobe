package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.ServerLevelData;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.bigglobe.versions.WorldPropertiesVersions;

@Mixin(MinecraftServer.class)
public class MinecraftServer_InitializeSpawnPoint {

	@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_setupSpawn(
		ServerLevel world,
		ServerLevelData worldProperties,
		boolean bonusChest,
		boolean debugWorld,
		LevelLoadListener loadProgress,
		CallbackInfo callback
	) {
		if (BigGlobeSpawnLocator.initWorldSpawn(world)) {
			if (bonusChest) {
				ConfiguredFeature<?, ?> feature = RegistryVersions.getObject(world.registryAccess(), MiscOverworldFeatures.BONUS_CHEST);
				if (feature != null) feature.place(
					world,
					world.getChunkSource().getGenerator(),
					world.getRandom(),
					WorldPropertiesVersions.getSpawnPos(worldProperties)
				);
			}
			callback.cancel();
		}
	}
}