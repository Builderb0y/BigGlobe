package builderb0y.bigglobe.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;

@AddPseudoField("conditions")
public class RandomizeDyeColorLootFunction extends ConditionalLootFunction {

	public static final Serializer SERIALIZER = new Serializer();

	public RandomizeDyeColorLootFunction(LootCondition[] conditions) {
		super(conditions);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (stack.getItem() instanceof DyeableItem item) {
			item.setColor(stack, context.getRandom().nextInt() & 0xFFFFFF);
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
			return new RandomizeDyeColorLootFunction(conditions);
		}
	}
}