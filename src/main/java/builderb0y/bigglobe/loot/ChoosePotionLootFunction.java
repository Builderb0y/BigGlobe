package builderb0y.bigglobe.loot;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.JsonHelper;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.IRandomList;

public class ChoosePotionLootFunction extends ConditionalLootFunction {

	public final IRandomList<Potion> potions;

	public ChoosePotionLootFunction(LootCondition[] conditions, IRandomList<Potion> potions) {
		super(conditions);
		this.potions = potions;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		PotionUtil.setPotion(stack, this.potions.getRandomElement(context.getRandom().nextLong()));
		return stack;
	}

	@Override
	public LootFunctionType getType() {
		return BigGlobeLoot.CHOOSE_POTION_TYPE;
	}

	public static class Serializer extends ConditionalLootFunction.Serializer<ChoosePotionLootFunction> {

		public static final AutoCoder<IRandomList<Potion>> POTION_LIST = (
			BigGlobeAutoCodec.AUTO_CODEC.createCoder(
				new ReifiedType<IRandomList<@UseName("potion") Potion>>() {}
			)
		);

		@Override
		public void toJson(JsonObject json, ChoosePotionLootFunction function, JsonSerializationContext context) {
			super.toJson(json, function, context);
			json.add("potions", BigGlobeAutoCodec.AUTO_CODEC.encode(POTION_LIST, function.potions, JsonOps.INSTANCE));
		}

		@Override
		public ChoosePotionLootFunction fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
			JsonArray potionsJson = JsonHelper.getArray(json, "potions");
			IRandomList<Potion> potions;
			try {
				potions = BigGlobeAutoCodec.AUTO_CODEC.decode(POTION_LIST, potionsJson, JsonOps.INSTANCE);
			}
			catch (DecodeException exception) {
				throw new JsonSyntaxException(exception);
			}
			return new ChoosePotionLootFunction(conditions, potions);
		}
	}
}