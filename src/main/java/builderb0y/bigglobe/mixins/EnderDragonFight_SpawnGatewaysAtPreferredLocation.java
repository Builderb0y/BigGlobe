package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EndDragonFight;

@Mixin(EndDragonFight.class)
public class EnderDragonFight_SpawnGatewaysAtPreferredLocation {

	@Shadow
	@Final
	private ServerLevel level;

	@ModifyConstant(method = "spawnNewGateway()V", constant = @Constant(doubleValue = 96.0D))
	private double bigglobe_overrideRadius(double oldValue) {
		if (this.level.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			return generator.end_overrides.inner_gateways().radius();
		}
		else {
			return oldValue;
		}
	}

	@ModifyConstant(method = "spawnNewGateway()V", constant = @Constant(intValue = 75))
	private int bigglobe_overrideHeight(int oldValue) {
		if (this.level.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.end_overrides != null) {
			return generator.end_overrides.inner_gateways().height();
		}
		else {
			return oldValue;
		}
	}
}