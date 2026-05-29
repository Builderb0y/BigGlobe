package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(CreakingHeartBlockEntity.class)
public class CreakingHeartBlockEntity_MakeWorkInTheNether {

	@ModifyReceiver(method = "updateCreakingState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;environmentAttributes()Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;"))
	private static Level bigglobe_makeWorkInNether(Level world) {
		if (world instanceof ServerLevel serverWorld && serverWorld.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.creaking_time_reference() != null) {
			ServerLevel replacement = serverWorld.getServer().getLevel(generator.game_mechanics.creaking_time_reference());
			if (replacement != null) return replacement;
		}
		return world;
	}

	@ModifyReceiver(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;environmentAttributes()Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;"))
	private static Level bigglobe_dontKillInNether(Level world) {
		if (world instanceof ServerLevel serverWorld && serverWorld.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.creaking_time_reference() != null) {
			ServerLevel replacement = serverWorld.getServer().getLevel(generator.game_mechanics.creaking_time_reference());
			if (replacement != null) return replacement;
		}
		return world;
	}
}