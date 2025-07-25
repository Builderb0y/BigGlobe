package builderb0y.bigglobe.loot;

import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.IRandomList;

@AddPseudoField("conditions")
public class ChoosePotionLootFunction extends ConditionalLootFunction {

	public static final LootFunctionType<ChoosePotionLootFunction> SERIALIZER = new LootFunctionType<>(BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ChoosePotionLootFunction.class));

	public final IRandomList<@UseName("potion") RegistryEntry<Potion>> potions;

	public ChoosePotionLootFunction(List<LootCondition> conditions, IRandomList<RegistryEntry<Potion>> potions) {
		super(conditions);
		this.potions = potions;
	}

	public @DefaultEmpty List<LootCondition> conditions() {
		return this.conditions;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(this.potions.getRandomElement(context.getRandom().nextLong())));
		return stack;
	}

	@Override
	public LootFunctionType<ChoosePotionLootFunction> getType() {
		return BigGlobeLoot.CHOOSE_POTION_TYPE;
	}
}