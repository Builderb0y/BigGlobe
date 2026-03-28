package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.items.BigGlobeItems;

@Mixin(ThrownEnderpearl.class)
public class EnderPearlEntity_ReduceFallDamageWithVoidmetalArmor {

	@ModifyExpressionValue(method = "onHit", at = @At(value = "CONSTANT", args = "floatValue=5.0"))
	private float bigglobe_reduceFallDamageWithVoidmetalArmor(float oldValue, @Local Entity entity) {
		ServerPlayer player = (ServerPlayer)(entity);
		int resistance = 0;
		if (player.getItemBySlot(EquipmentSlot.FEET).getItem() == BigGlobeItems.VOIDMETAL_BOOTS) resistance += 4;
		if (player.getItemBySlot(EquipmentSlot.LEGS).getItem() == BigGlobeItems.VOIDMETAL_LEGGINGS) resistance += 3;
		if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() == BigGlobeItems.VOIDMETAL_CHESTPLATE) resistance += 2;
		if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == BigGlobeItems.VOIDMETAL_HELMET) resistance += 1;
		return oldValue * (10 - resistance) / 10.0F;
	}
}