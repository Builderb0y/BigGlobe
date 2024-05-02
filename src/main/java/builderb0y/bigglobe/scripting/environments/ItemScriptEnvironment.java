package builderb0y.bigglobe.scripting.environments;

import java.util.random.RandomGenerator;

import net.minecraft.item.Item;

import builderb0y.bigglobe.scripting.wrappers.ItemStackWrapper;
import builderb0y.bigglobe.scripting.wrappers.ItemTagKey;
import builderb0y.bigglobe.scripting.wrappers.ItemWrapper;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;

public class ItemScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addType("Item", ItemWrapper.TYPE)
		.addType("ItemStack", ItemStackWrapper.TYPE)
		.addType("ItemTag", ItemTagKey.TYPE)
		.addCastConstant(ItemWrapper.CONSTANT_FACTORY, true)
		.addCastConstant(ItemTagKey.CONSTANT_FACTORY, true)
		.addQualifiedFunctionRenamedMultiInvokeStatic(ItemStackWrapper.TYPE, ItemStackWrapper.class, "new", "create")

		.addFieldInvokeStatic(ItemWrapper.class, "id")
		.addMethodInvokeStatics(ItemWrapper.class, "isIn", "getDefaultStack")

		.addMethodInvokeSpecific(ItemTagKey.class, "random", Item.class, RandomGenerator.class)
		.addMethodInvokeSpecific(ItemTagKey.class, "random", Item.class, long.class)

		.addQualifiedVariableGetStatic(ItemStackWrapper.TYPE, ItemStackWrapper.class, "EMPTY")
		.addFieldInvokeStatic(ItemStackWrapper.class, "item")
		.addFieldInvokeStatics(ItemStackWrapper.class, "empty", "maxCount", "stackable", "count")
		.addFieldInvokeStatics(ItemStackWrapper.class, "damage", "maxDamage", "damageable")
		.addFieldInvokeStatic(ItemStackWrapper.class, "nbt")
	);

	public static MutableScriptEnvironment createWithRandom(InsnTree loadRandom) {
		return (
			new MutableScriptEnvironment()
			.addAll(INSTANCE)
			.addMethod(ItemTagKey.TYPE, "random", MinecraftScriptEnvironment.tagRandom(loadRandom, ItemTagKey.class, Item.class))
		);
	}
}