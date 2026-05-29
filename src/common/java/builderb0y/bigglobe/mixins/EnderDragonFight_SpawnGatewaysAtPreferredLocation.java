package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(EnderDragonFight.class)
public class EnderDragonFight_SpawnGatewaysAtPreferredLocation {

	@Shadow
	@Final
	private ServerLevel level;

	@ModifyConstant(method = "spawnNewGateway()V", constant = @Constant(doubleValue = 96.0D))
	private double bigglobe_overrideRadius(double oldValue) {
		if (this.level.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
			return generator.game_mechanics.end().inner_gateways().radius();
		}
		else {
			return oldValue;
		}
	}

	@ModifyConstant(method = "spawnNewGateway()V", constant = @Constant(intValue = 75))
	private int bigglobe_overrideHeight(int oldValue) {
		if (this.level.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.game_mechanics.end() != null) {
			return generator.game_mechanics.end().inner_gateways().height();
		}
		else {
			return oldValue;
		}
	}
}