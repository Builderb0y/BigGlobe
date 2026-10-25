package builderb0y.bigglobe.versions;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("UnnecessaryFullyQualifiedName")
public class ActionResultVersions {

	public static final InteractionResult
		SUCCESS = InteractionResult.SUCCESS,
		CONSUME = InteractionResult.CONSUME,
		PASS    = InteractionResult.PASS,
		FAIL    = InteractionResult.FAIL;

	public static final ItemInteractionResult
		ITEM_SUCCESS = ItemInteractionResult.SUCCESS,
		ITEM_CONSUME = ItemInteractionResult.CONSUME,
		ITEM_PASS    = ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
		ITEM_FAIL    = ItemInteractionResult.FAIL;

	public static InteractionResultHolder<ItemStack> typedSuccess(ItemStack stack) {
		return InteractionResultHolder.success(stack);
	}

	public static InteractionResultHolder<ItemStack> typedConsume(ItemStack stack) {
		return InteractionResultHolder.consume(stack);
	}

	public static InteractionResultHolder<ItemStack> typePass(ItemStack stack) {
		return InteractionResultHolder.pass(stack);
	}

	public static InteractionResultHolder<ItemStack> typedFail(ItemStack stack) {
		return InteractionResultHolder.fail(stack);
	}
}
