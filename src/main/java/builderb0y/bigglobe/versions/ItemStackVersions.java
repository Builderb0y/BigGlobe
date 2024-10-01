package builderb0y.bigglobe.versions;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import builderb0y.bigglobe.items.DynamicMaxDamageItem;

#if MC_VERSION >= MC_1_20_5
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Hand;

import builderb0y.bigglobe.BigGlobeMod;
#endif

public class ItemStackVersions {

	public static int getMaxDamage(ItemStack stack) {
		#if MC_VERSION >= MC_1_20_5
			return stack.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
		#else
			return stack.getMaxDamage();
		#endif
	}

	public static int getDamage(ItemStack stack) {
		#if MC_VERSION >= MC_1_20_5
			return stack.getOrDefault(DataComponentTypes.DAMAGE, 0);
		#else
			NbtCompound nbt = stack.getNbt();
			return nbt != null ? nbt.getInt(ItemStack.DAMAGE_KEY) : 0;
		#endif
	}

	public static void setMaxDamage(ItemStack stack, int maxDamage) {
		#if MC_VERSION >= MC_1_20_5
			stack.set(DataComponentTypes.MAX_DAMAGE, maxDamage);
		#else
			if (stack.getItem() instanceof DynamicMaxDamageItem dynamic) {
				dynamic.bigglobe_setMaxDamage(stack, maxDamage);
			}
		#endif
	}

	public static void setDamage(ItemStack stack, int damage) {
		#if MC_VERSION >= MC_1_20_5
			stack.set(DataComponentTypes.DAMAGE, damage);
		#else
			stack.getOrCreateNbt().putInt(ItemStack.DAMAGE_KEY, damage);
		#endif
	}

	public static Text getCustomName(ItemStack stack) {
		#if MC_VERSION >= MC_1_20_5
			return stack.get(DataComponentTypes.CUSTOM_NAME);
		#else
			return stack.hasCustomName() ? stack.getName() : null;
		#endif
	}

	public static void setCustomName(ItemStack stack, Text name) {
		#if MC_VERSION >= MC_1_20_5
			stack.set(DataComponentTypes.CUSTOM_NAME, name);
		#else
			stack.setCustomName(name);
		#endif
	}

	public static void damage(ItemStack stack, PlayerEntity player, Hand hand) {
		#if MC_VERSION >= MC_1_20_5
			stack.damage(1, player, LivingEntity.getSlotForHand(hand));
		#else
			stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
		#endif
	}

	public static NbtCompound toNbt(ItemStack stack) {
		#if MC_VERSION >= MC_1_20_5
			return (NbtCompound)(stack.encode(BigGlobeMod.getCurrentServer().getRegistryManager()));
		#else
			return stack.writeNbt(new NbtCompound());
		#endif
	}

	public static void toNbt(ItemStack stack, NbtCompound nbt) {
		#if MC_VERSION >= MC_1_20_5
			nbt.copyFrom((NbtCompound)(stack.encode(BigGlobeMod.getCurrentServer().getRegistryManager())));
		#else
			stack.writeNbt(nbt);
		#endif
	}

	public static ItemStack fromNbt(NbtCompound nbt) {
		#if MC_VERSION >= MC_1_20_5
			return ItemStack.fromNbtOrEmpty(BigGlobeMod.getCurrentServer().getRegistryManager(), nbt);
		#else
			return ItemStack.fromNbt(nbt);
		#endif
	}
}