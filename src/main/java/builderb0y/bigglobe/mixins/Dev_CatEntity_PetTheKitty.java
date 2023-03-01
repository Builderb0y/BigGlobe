package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
in vanilla, you can't pet cats. this is obviously a bug, so I'm fixing it.
if anyone finds this code and wants it in production,
let me know and I'll make a separate mod for it.
*/
@Mixin(CatEntity.class)
public abstract class Dev_CatEntity_PetTheKitty extends TameableEntity {

	public Dev_CatEntity_PetTheKitty(EntityType<? extends TameableEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
	public void bigglobe_petTheKitty(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
		if (player.isSneaking() && player.getStackInHand(hand).isEmpty()) {
			if (!this.world.isClient) {
				this.world.sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
				this.playSound(SoundEvents.ENTITY_CAT_PURR, this.getSoundVolume(), this.getSoundPitch());
			}
			callback.setReturnValue(ActionResult.SUCCESS);
		}
	}
}