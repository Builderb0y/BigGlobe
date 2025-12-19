package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.ServerWorldAccess;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

#if MC_VERSION >= MC_1_21_11

	@Mixin(HostileEntity.class)
	public class HuskEntity_AllowSpawningUndergroundInBigGlobeWorlds {

		@ModifyExpressionValue(method = "canSpawnInDarkUnderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorldAccess;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
		private static boolean bigglobe_allowHusksToSpawnUndergroundInBigGlobeWorlds(boolean original, @Local(argsOnly = true) ServerWorldAccess world) {
			return original || ((ServerChunkManager)(world.getChunkManager())).getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator;
		}
	}

#else

	@Mixin(HuskEntity.class)
	public class HuskEntity_AllowSpawningUndergroundInBigGlobeWorlds {

		@ModifyExpressionValue(method = "canSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorldAccess;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
		private static boolean bigglobe_allowHusksToSpawnUndergroundInBigGlobeWorlds(boolean original, @Local(argsOnly = true) ServerWorldAccess world) {
			return original || ((ServerChunkManager)(world.getChunkManager())).getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator;
		}
	}

#endif