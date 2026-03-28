package builderb0y.bigglobe.versions;

import net.minecraft.world.item.ItemStack;

@SuppressWarnings("UnnecessaryFullyQualifiedName")
public class ActionResultVersions {

	public static final net.minecraft.world.InteractionResult
		SUCCESS = net.minecraft.world.InteractionResult.SUCCESS,
		CONSUME = net.minecraft.world.InteractionResult.CONSUME,
		PASS = net.minecraft.world.InteractionResult.PASS,
		FAIL = net.minecraft.world.InteractionResult.FAIL,

	ITEM_SUCCESS = net.minecraft.world.InteractionResult.SUCCESS,
		ITEM_CONSUME = net.minecraft.world.InteractionResult.CONSUME,
		ITEM_PASS = net.minecraft.world.InteractionResult.PASS,
		ITEM_FAIL = net.minecraft.world.InteractionResult.FAIL;

	public static net.minecraft.world.InteractionResult typedSuccess(ItemStack stack) {
		return net.minecraft.world.InteractionResult.SUCCESS.heldItemTransformedTo(stack);
	}

	public static net.minecraft.world.InteractionResult typedConsume(ItemStack stack) {
		return net.minecraft.world.InteractionResult.CONSUME.heldItemTransformedTo(stack);
	}

	public static net.minecraft.world.InteractionResult typePass(ItemStack stack) {
		return net.minecraft.world.InteractionResult.PASS;
	}

	public static net.minecraft.world.InteractionResult typedFail(ItemStack stack) {
		return net.minecraft.world.InteractionResult.FAIL;
	}
}