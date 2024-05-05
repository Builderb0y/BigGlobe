package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.items.BigGlobeItems;

@Mixin(EnderPearlEntity.class)
public class EnderPearlEntity_ReduceFallDamageWithVoidmetalArmor {

	@ModifyExpressionValue(method = "onCollision", at = @At(value = "CONSTANT", args = "floatValue=5.0"))
	private float bigglobe_reduceFallDamageWithVoidmetalArmor(float oldValue, @Local Entity entity) {
		ServerPlayerEntity player = (ServerPlayerEntity)(entity);
		int resistance = 0;
		if (player.getEquippedStack(EquipmentSlot.FEET ).getItem() == BigGlobeItems.VOIDMETAL_BOOTS     ) resistance += 4;
		if (player.getEquippedStack(EquipmentSlot.LEGS ).getItem() == BigGlobeItems.VOIDMETAL_LEGGINGS  ) resistance += 3;
		if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() == BigGlobeItems.VOIDMETAL_CHESTPLATE) resistance += 2;
		if (player.getEquippedStack(EquipmentSlot.HEAD ).getItem() == BigGlobeItems.VOIDMETAL_HELMET    ) resistance += 1;
		return oldValue * (10 - resistance) / 10.0F;
	}
}