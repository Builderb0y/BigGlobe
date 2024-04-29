package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;

import builderb0y.bigglobe.entities.WaypointEntity;

@Mixin(ProjectileEntity.class)
public class ProjectileEntity_DontHitWaypoints {

	@Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
	private void bigglobe_dontHitWaypoints(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (entity instanceof WaypointEntity) callback.setReturnValue(Boolean.FALSE);
	}
}