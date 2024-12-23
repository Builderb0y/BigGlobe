package builderb0y.bigglobe.loot;

import java.util.List;

import net.minecraft.item.DyeableItem;
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

	public static final LootFunctionType SERIALIZER = new LootFunctionType(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(RandomizeDyeColorLootFunction.class));

	public final boolean show_in_tooltip;

	public RandomizeDyeColorLootFunction(List<LootCondition> conditions, boolean show_in_tooltip) {
		super(conditions);
		this.show_in_tooltip = show_in_tooltip;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (stack.getItem() instanceof DyeableItem item) {
			item.setColor(stack, context.getRandom().nextInt() & 0xFFFFFF);
			if (!this.show_in_tooltip) stack.addHideFlag(TooltipSection.DYE);
		}
		return stack;
	}

	public @DefaultEmpty List<LootCondition> conditions() {
		return this.conditions;
	}

	@Override
	public LootFunctionType getType() {
		return BigGlobeLoot.RANDOMIZE_DYE_COLOR;
	}
}