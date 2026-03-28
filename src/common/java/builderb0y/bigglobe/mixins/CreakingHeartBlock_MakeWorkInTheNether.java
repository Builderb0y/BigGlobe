package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CreakingHeartBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(CreakingHeartBlock.class)
public class CreakingHeartBlock_MakeWorkInTheNether {

	@ModifyReceiver(method = "updateState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;environmentAttributes()Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;"))
	private static Level bigglobe_makeWorkInNether(Level world) {
		if (world instanceof ServerLevel serverWorld && serverWorld.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.creaking_overrides != null) {
			ServerLevel replacement = serverWorld.getServer().getLevel(generator.creaking_overrides.time_reference());
			if (replacement != null) return replacement;
		}
		return world;
	}
}