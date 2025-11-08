package builderb0y.bigglobe.mixins;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator.SpawnPoint;
import builderb0y.bigglobe.versions.GameProfileVersions;

#if MC_VERSION >= MC_1_21_9
	@Mixin(net.minecraft.server.network.PrepareSpawnTask.class)
#else
	@Mixin(PlayerManager.class)
#endif
public abstract class PlayerManager_InitializeSpawnPoint {

	#if MC_VERSION >= MC_1_21_9

		@Shadow @Final private net.minecraft.server.PlayerConfigEntry player;

		@ModifyArg(method = "sendPacket", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseGet(Ljava/util/function/Supplier;)Ljava/lang/Object;", ordinal = 1))
		private Supplier<CompletableFuture<Vec3d>> bigglobe_setPerPlayerSpawnIfEnabled(Supplier<CompletableFuture<Vec3d>> original, @Local ServerWorld serverWorld) {
			if (
				BigGlobeConfig.INSTANCE.get().playerSpawning.perPlayerSpawnPoints &&
				serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator
			) {
				net.minecraft.server.PlayerConfigEntry player = this.player;
				return () -> CompletableFuture.supplyAsync(() -> {
					SpawnPoint spawnPoint = BigGlobeSpawnLocator.findSpawn(serverWorld, generator, BigGlobeSpawnLocator.perPlayerSeed(serverWorld, player.id()));
					return spawnPoint != null ? new Vec3d(spawnPoint.x(), spawnPoint.y(), spawnPoint.z()) : null;
				})
				.thenCompose((Vec3d spawnPos) -> {
					return spawnPos != null ? CompletableFuture.completedFuture(spawnPos) : original.get();
				});
			}
			else {
				return original;
			}
		}

	#else

		@Inject(method = "loadPlayerData", at = @At("RETURN"))
		private void bigglobe_setPerPlayerSpawnIfEnabled(
			ServerPlayerEntity player,
			#if MC_VERSION >= MC_1_21_6
				net.minecraft.util.ErrorReporter errorReporter,
			#endif
			CallbackInfoReturnable<Optional<?>> callback
		) {
			if (callback.getReturnValue() == null) { //player's first time on the server.
				BigGlobeSpawnLocator.initPlayerSpawn(player);
			}
		}

	#endif
}