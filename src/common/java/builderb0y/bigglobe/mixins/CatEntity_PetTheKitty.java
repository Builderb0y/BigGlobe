package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.versions.EntityVersions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
in vanilla, you can't pet cats. this is obviously a bug, so I'm fixing it.
if anyone finds this code and wants it in production,
let me know and I'll make a separate mod for it.
*/
@Mixin(Cat.class)
public abstract class CatEntity_PetTheKitty extends TamableAnimal {

	public CatEntity_PetTheKitty(EntityType<? extends TamableAnimal> entityType, Level world) {
		super(entityType, world);
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	public void bigglobe_petTheKitty(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (EntityVersions.getWorld(this).isClientSide()) {
				this.handleEntityEvent(EntityEvent.TAMING_SUCCEEDED);
				EntityVersions.getWorld(this).playSound(
					player,
					this.getX(),
					this.getY(),
					this.getZ(),
					SoundEvents.CAT_PURR_BABY,
					this.getSoundSource(),
					this.getSoundVolume(),
					this.getVoicePitch()
				);
			}
			callback.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}