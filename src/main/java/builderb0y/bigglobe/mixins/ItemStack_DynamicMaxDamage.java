package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import builderb0y.bigglobe.items.DynamicMaxDamageItem;

@Mixin(ItemStack.class)
public abstract class ItemStack_DynamicMaxDamage {

	@Shadow public abstract Item getItem();

	@Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
	private void bigglobe_overrideMaxDamage(CallbackInfoReturnable<Integer> callback) {
		if (this.getItem() instanceof DynamicMaxDamageItem dynamic) {
			callback.setReturnValue(dynamic.bigglobe_getMaxDamage((ItemStack)(Object)(this)));
		}
	}
}