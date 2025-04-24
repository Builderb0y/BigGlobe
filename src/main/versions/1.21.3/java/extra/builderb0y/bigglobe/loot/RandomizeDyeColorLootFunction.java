package builderb0y.bigglobe.loot;

import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("conditions")
public class RandomizeDyeColorLootFunction extends ConditionalLootFunction {

	public static final LootFunctionType<RandomizeDyeColorLootFunction> SERIALIZER = new LootFunctionType<>(BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RandomizeDyeColorLootFunction.class));

	public RandomizeDyeColorLootFunction(List<LootCondition> conditions) {
		super(conditions);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(context.getRandom().nextInt() & 0xFFFFFF, false));
		return stack;
	}

	public @DefaultEmpty List<LootCondition> conditions() {
		return this.conditions;
	}

	@Override
	public LootFunctionType<? extends ConditionalLootFunction> getType() {
		return BigGlobeLoot.RANDOMIZE_DYE_COLOR;
	}
}