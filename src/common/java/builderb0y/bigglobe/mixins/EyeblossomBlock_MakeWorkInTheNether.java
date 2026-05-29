package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.EyeblossomBlock;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(EyeblossomBlock.class)
public class EyeblossomBlock_MakeWorkInTheNether {

	@ModifyReceiver(
		method = "tryChangingState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;environmentAttributes()Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;")
	)
	private ServerLevel bigglobe_useAlternateWorldIfDesired(ServerLevel original) {
		ServerLevel replacement;
		return (
			original.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.game_mechanics.creaking_time_reference() != null &&
			(replacement = original.getServer().getLevel(generator.game_mechanics.creaking_time_reference())) != null
			? replacement
			: original
		);
	}
}