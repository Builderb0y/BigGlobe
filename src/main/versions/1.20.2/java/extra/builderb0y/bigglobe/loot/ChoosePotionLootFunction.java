package builderb0y.bigglobe.loot;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.IRandomList;

@AddPseudoField("conditions")
public class ChoosePotionLootFunction extends ConditionalLootFunction {

	public static final LootFunctionType SERIALIZER = new LootFunctionType(BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ChoosePotionLootFunction.class).codec());

	public final IRandomList<@UseName("potion") Potion> potions;

	public ChoosePotionLootFunction(List<LootCondition> conditions, IRandomList<Potion> potions) {
		super(conditions);
		this.potions = potions;
	}

	public @DefaultEmpty List<LootCondition> conditions() {
		return this.conditions;
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
}