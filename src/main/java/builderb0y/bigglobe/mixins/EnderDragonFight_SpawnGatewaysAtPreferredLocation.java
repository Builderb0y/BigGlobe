package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(EnderDragonFight.class)
public class EnderDragonFight_SpawnGatewaysAtPreferredLocation {

	@Shadow @Final private ServerWorld world;

	@ModifyConstant(method = "generateNewEndGateway", constant = @Constant(doubleValue = 96.0D))
	private double bigglobe_overrideRadius(double oldValue) {
		if (this.world.getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			return generator.end_overrides.inner_gateways().radius();
		}
		else {
			return oldValue;
		}
	}

	@ModifyConstant(method = "generateNewEndGateway", constant = @Constant(intValue = 75))
	private int bigglobe_overrideHeight(int oldValue) {
		if (this.world.getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			return generator.end_overrides.inner_gateways().height();
		}
		else {
			return oldValue;
		}
	}
}