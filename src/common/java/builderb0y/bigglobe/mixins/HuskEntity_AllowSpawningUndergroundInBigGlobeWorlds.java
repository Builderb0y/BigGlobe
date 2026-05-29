package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(Monster.class)
public class HuskEntity_AllowSpawningUndergroundInBigGlobeWorlds {

	@ModifyExpressionValue(method = "checkSurfaceMonstersSpawnRules", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"))
	private static boolean bigglobe_allowHusksToSpawnUndergroundInBigGlobeWorlds(boolean original, @Local(argsOnly = true) ServerLevelAccessor world) {
		return original || ((ServerChunkCache)(world.getChunkSource())).getGenerator() instanceof BigGlobeScriptedChunkGenerator;
	}
}