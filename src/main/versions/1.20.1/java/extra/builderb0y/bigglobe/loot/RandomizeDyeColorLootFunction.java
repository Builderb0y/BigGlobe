package builderb0y.bigglobe.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStack.TooltipSection;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.JsonHelper;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;

@AddPseudoField("conditions")
public class RandomizeDyeColorLootFunction extends ConditionalLootFunction {

	public static final Serializer SERIALIZER = new Serializer();

	public final boolean show_in_tooltip;

	public RandomizeDyeColorLootFunction(LootCondition[] conditions, boolean show_in_tooltip) {
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

	public @DefaultEmpty LootCondition[] conditions() {
		return this.conditions;
	}

	@Override
	public LootFunctionType getType() {
		return BigGlobeLoot.RANDOMIZE_DYE_COLOR;
	}

	public static class Serializer extends ConditionalLootFunction.Serializer<RandomizeDyeColorLootFunction> {

		@Override
		public RandomizeDyeColorLootFunction fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
			return new RandomizeDyeColorLootFunction(conditions, JsonHelper.getBoolean(json, "show_in_tooltip"));
		}

		@Override
		public void toJson(JsonObject jsonObject, RandomizeDyeColorLootFunction function, JsonSerializationContext jsonSerializationContext) {
			super.toJson(jsonObject, function, jsonSerializationContext);
			jsonObject.addProperty("show_in_tooltip", function.show_in_tooltip);
		}
	}
}