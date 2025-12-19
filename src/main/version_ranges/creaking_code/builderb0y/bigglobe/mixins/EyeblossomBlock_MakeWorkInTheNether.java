package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.block.EyeblossomBlock;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(EyeblossomBlock.class)
public class EyeblossomBlock_MakeWorkInTheNether {

	@ModifyReceiver(
		method = "updateStateAndNotifyOthers",
		at = {
			#if MC_VERSION >= MC_1_21_11
				@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getEnvironmentAttributes()Lnet/minecraft/world/attribute/WorldEnvironmentAttributeAccess;")
			#else
				@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getDimension()Lnet/minecraft/world/dimension/DimensionType;"),
				@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isDay()Z")
			#endif
		}
	)
	private ServerWorld bigglobe_useAlternateWorldIfDesired(ServerWorld original) {
		ServerWorld replacement;
		return (
			original.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.creaking_overrides != null &&
			(replacement = original.getServer().getWorld(generator.creaking_overrides.time_reference())) != null
			? replacement
			: original
		);
	}
}