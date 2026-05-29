package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import builderb0y.bigglobe.hyperspace.HyperspaceFlight;

@Mixin(Player.class)
public abstract class PlayerEntity_FlyInHyperspace extends LivingEntity {

	public PlayerEntity_FlyInHyperspace(EntityType<? extends LivingEntity> entityType, Level world) {
		super(entityType, world);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void bigglobe_flyInHyperspace(CallbackInfo callback) {
		HyperspaceFlight.onPlayerTick((Player)(Object)(this));
	}
}