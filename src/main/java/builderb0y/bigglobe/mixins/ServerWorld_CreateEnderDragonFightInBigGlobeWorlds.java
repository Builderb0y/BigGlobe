package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;

import builderb0y.bigglobe.dimensionTypes.BigGlobeDimensionTypeKeys;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_CreateEnderDragonFightInBigGlobeWorlds extends World {

	@Shadow private @Nullable EnderDragonFight enderDragonFight;

	public ServerWorld_CreateEnderDragonFightInBigGlobeWorlds() {
		super(null, null, null, null, null, false, false, 0L, 0);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_createEnderDragonFight(
		MinecraftServer server,
		Executor workerExecutor,
		Session session,
		ServerWorldProperties properties,
		RegistryKey<World> worldKey,
		DimensionOptions dimensionOptions,
		WorldGenerationProgressListener worldGenerationProgressListener,
		boolean debugWorld,
		long seed,
		List spawners,
		boolean shouldTickTime,
		RandomSequencesState randomSequencesState,
		CallbackInfo callback
	) {
		if (this.getRegistryKey() == World.END && this.getDimensionEntry().matchesKey(BigGlobeDimensionTypeKeys.END)) {
			this.enderDragonFight = new EnderDragonFight((ServerWorld)(Object)(this), server.getSaveProperties().getGeneratorOptions().getSeed(), server.getSaveProperties().getDragonFight());
		}
	}
}