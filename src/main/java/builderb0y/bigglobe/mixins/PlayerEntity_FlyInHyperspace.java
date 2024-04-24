package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import builderb0y.bigglobe.hyperspace.HyperspaceConstants;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_FlyInHyperspace extends LivingEntity {

	@Shadow public abstract PlayerAbilities getAbilities();

	public PlayerEntity_FlyInHyperspace(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void bigglobe_flyInHyperspace(CallbackInfo callback) {
		if (this.getWorld().getRegistryKey() == HyperspaceConstants.WORLD_KEY) {
			this.getAbilities().allowFlying = true;
			this.getAbilities().flying = true;
		}
	}
}