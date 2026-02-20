package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;

import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_TickHyperspaceCollapse {

	@Inject(method = "tick", at = @At("RETURN"))
	private void bigglobe_tickHyperspaceCollapse(CallbackInfo callback) {
		PlayerWaypointManager manager = PlayerWaypointManager.get((PlayerEntity)(Object)(this));
		if (manager != null) manager.tick();
	}
}