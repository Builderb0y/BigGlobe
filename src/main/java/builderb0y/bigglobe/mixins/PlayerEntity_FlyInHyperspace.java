package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import builderb0y.bigglobe.hyperspace.HyperspaceFlight;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_FlyInHyperspace extends LivingEntity {

	public PlayerEntity_FlyInHyperspace(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void bigglobe_flyInHyperspace(CallbackInfo callback) {
		HyperspaceFlight.onPlayerTick((PlayerEntity)(Object)(this));
	}
}