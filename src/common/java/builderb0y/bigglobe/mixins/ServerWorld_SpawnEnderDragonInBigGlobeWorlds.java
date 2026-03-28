package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(ServerLevel.class)
public abstract class ServerWorld_SpawnEnderDragonInBigGlobeWorlds extends Level {

	@Shadow
	private @Nullable EndDragonFight dragonFight;

	public ServerWorld_SpawnEnderDragonInBigGlobeWorlds() {
		super(null, null, null, null, false, false, 0L, 0);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_createEnderDragonFight(
		CallbackInfo callback,
		@Local(argsOnly = true) MinecraftServer server,
		@Local(argsOnly = true) LevelStem dimensionOptions
	) {
		if (this.dragonFight == null && dimensionOptions.generator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			this.dragonFight = new EndDragonFight(
				(ServerLevel)(Object)(this),
				server.getWorldData().worldGenOptions().seed(),
				server.getWorldData().endDragonFightData()
			);
		}
	}
}