package builderb0y.bigglobe.scripting.environments;

import java.util.random.RandomGenerator;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import builderb0y.bigglobe.scripting.wrappers.ItemStackWrapper;
import builderb0y.bigglobe.scripting.wrappers.ItemTagKey;
import builderb0y.bigglobe.scripting.wrappers.ItemWrapper;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

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
		.addMethodInvokeStatic(ItemStackWrapper.class, "copy")
		.addFieldInvokeStatics(ItemStackWrapper.class, "empty", "maxCount", "stackable")
		.addFieldGetterSetterStatic(ItemStack.class, ItemStackWrapper.class, "count", int.class)
		.addFieldInvokeStatics(ItemStackWrapper.class, "maxDamage", "damageable")
		.addFieldGetterSetterStatic(ItemStack.class, ItemStackWrapper.class, "damage", int.class)
		.addFieldGetterSetterStatic(ItemStack.class, ItemStackWrapper.class, "nbt", NbtCompound.class)
	);

	public static MutableScriptEnvironment createWithRandom(InsnTree loadRandom) {
		return (
			new MutableScriptEnvironment()
			.addAll(INSTANCE)
			.addMethod(ItemTagKey.TYPE, "random", MinecraftScriptEnvironment.tagRandom(loadRandom, ItemTagKey.class, Item.class))
		);
	}
}