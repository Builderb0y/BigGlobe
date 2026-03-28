package builderb0y.bigglobe.versions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import builderb0y.autocodec.util.DFUVersions;
import builderb0y.bigglobe.BigGlobeMod;

public class ItemStackVersions {

	public static int getMaxDamage(ItemStack stack) {

		return stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
	}

	public static int getDamage(ItemStack stack) {

		return stack.getOrDefault(DataComponents.DAMAGE, 0);
	}

	public static void setMaxDamage(ItemStack stack, int maxDamage) {

		stack.set(DataComponents.MAX_DAMAGE, maxDamage);
	}

	public static void setDamage(ItemStack stack, int damage) {

		stack.set(DataComponents.DAMAGE, damage);
	}

	public static Component getCustomName(ItemStack stack) {

		return stack.get(DataComponents.CUSTOM_NAME);
	}

	public static void setCustomName(ItemStack stack, Component name) {

		stack.set(DataComponents.CUSTOM_NAME, name);
	}

	public static void damage(ItemStack stack, Player player, InteractionHand hand) {

		stack.hurtAndBreak(1, player, hand);
	}

	public static CompoundTag toNbt(ItemStack stack) {

		return (CompoundTag)(ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, BigGlobeMod.getCurrentServer().registryAccess()), stack).getOrThrow());
	}

	public static void toNbt(ItemStack stack, CompoundTag nbt) {

		nbt.store(ItemStack.MAP_CODEC, RegistryOps.create(NbtOps.INSTANCE, BigGlobeMod.getCurrentServer().registryAccess()), stack);
	}

	public static ItemStack fromNbt(CompoundTag nbt) {

		ItemStack stack = DFUVersions.getResult(
			ItemStack.CODEC.parse(
				BigGlobeMod
					.getCurrentServer()
					.registryAccess()
					.createSerializationContext(NbtOps.INSTANCE),
				nbt
			)
		);
		return stack != null ? stack : ItemStack.EMPTY;
	}
}