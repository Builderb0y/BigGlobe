package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;

import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;

@Mixin(Player.class)
public abstract class PlayerEntity_TickHyperspaceCollapse {

	@Inject(method = "tick", at = @At("RETURN"))
	private void bigglobe_tickHyperspaceCollapse(CallbackInfo callback) {
		PlayerWaypointManager manager = PlayerWaypointManager.get((Player)(Object)(this));
		if (manager != null) manager.tick();
	}
}