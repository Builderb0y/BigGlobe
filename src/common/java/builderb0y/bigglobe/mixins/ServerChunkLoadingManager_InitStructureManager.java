package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(ChunkMap.class)
public class ServerChunkLoadingManager_InitStructureManager {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;createState(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;", shift = Shift.AFTER))
	private void bigglobe_initStructureManager(
		CallbackInfo callback,
		@Local(argsOnly = true) ServerLevel world,
		@Local(argsOnly = true) ChunkGenerator chunkGenerator
	) {
		if (chunkGenerator instanceof BigGlobeScriptedChunkGenerator generator) {
			generator.setStructuresEnabled(world.getServer().getWorldGenSettings().options().generateStructures());
		}
	}
}