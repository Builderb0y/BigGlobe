package builderb0y.bigglobe.mixins;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator.SpawnPoint;

@Mixin(PrepareSpawnTask.class)
public abstract class PlayerManager_InitializeSpawnPoint {

	@Shadow
	@Final
	NameAndId nameAndId;

	@ModifyArg(method = "start", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseGet(Ljava/util/function/Supplier;)Ljava/lang/Object;", ordinal = 1))
	private Supplier<CompletableFuture<Vec3>> bigglobe_setPerPlayerSpawnIfEnabled(Supplier<CompletableFuture<Vec3>> original, @Local ServerLevel serverWorld) {
		if (
			BigGlobeConfig.INSTANCE.get().playerSpawning.perPlayerSpawnPoints &&
			serverWorld.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator
		) {
			NameAndId player = this.nameAndId;
			return () -> CompletableFuture.supplyAsync(() -> {
				SpawnPoint spawnPoint = BigGlobeSpawnLocator.findSpawn(serverWorld, generator, BigGlobeSpawnLocator.perPlayerSeed(serverWorld, player.id()));
				return spawnPoint != null ? new Vec3(spawnPoint.x(), spawnPoint.y(), spawnPoint.z()) : null;
			})
			.thenCompose((Vec3 spawnPos) -> {
				return spawnPos != null ? CompletableFuture.completedFuture(spawnPos) : original.get();
			});
		}
		else {
			return original;
		}
	}
}