package builderb0y.bigglobe.scripting.environments;

import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import net.minecraft.world.item.Item;
import builderb0y.bigglobe.scripting.wrappers.ItemStackWrapper;
import builderb0y.bigglobe.scripting.wrappers.ItemWrapper;
import builderb0y.bigglobe.scripting.wrappers.tags.ItemTag;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;

public class ItemScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
			.addType("Item", ItemWrapper.TYPE)
			.addType("ItemStack", ItemStackWrapper.TYPE)
			.addType("ItemTag", ItemTag.TYPE)
			.addCastConstant(ItemWrapper.CONSTANT_FACTORY, true)
			.configure(ItemTag.PARSER)
			.addQualifiedFunctionRenamedMultiInvokeStatic(ItemStackWrapper.TYPE, ItemStackWrapper.class, "new", "create")

			.addFieldInvokeStatic(ItemWrapper.class, "id")
			.addMethodInvokeStatics(ItemWrapper.class, "getDefaultStack")

			.addMethodInvokeSpecific(ItemTag.class, "random", Item.class, RandomGenerator.class)
			.addMethodInvokeSpecific(ItemTag.class, "random", Item.class, long.class)

			.addQualifiedVariableGetStatic(ItemStackWrapper.TYPE, ItemStackWrapper.class, "EMPTY")
			.addFieldInvokeStatic(ItemStackWrapper.class, "item")
			.addFieldInvokeStatics(ItemStackWrapper.class, "empty", "maxCount", "stackable", "count")
			.addFieldInvokeStatics(ItemStackWrapper.class, "damage", "maxDamage", "damageable")
			.addFieldInvokeStatic(ItemStackWrapper.class, "nbt")
			.addMethod(ItemStackWrapper.TYPE, "isIn", ItemStackWrapper.TAG_PARSER.makeIsIn())
	);

	public static Consumer<MutableScriptEnvironment> createWithRandom(InsnTree loadRandom) {
		return (MutableScriptEnvironment environment) -> {
			environment
				.addAll(INSTANCE)
				.addMethod(ItemTag.TYPE, "random", MinecraftScriptEnvironment.tagRandom(loadRandom, ItemTag.class, Item.class))
			;
		};
	}
}