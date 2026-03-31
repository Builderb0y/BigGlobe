package builderb0y.bigglobe.loot;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.IRandomList;

@AddPseudoField("conditions")
public class ChoosePotionLootFunction extends LootItemConditionalFunction {

	public static final MapCodec<ChoosePotionLootFunction> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ChoosePotionLootFunction.class);

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return CODEC;
	}

	public final IRandomList<@UseName("potion") Holder<Potion>> potions;

	public ChoosePotionLootFunction(List<LootItemCondition> conditions, IRandomList<Holder<Potion>> potions) {
		super(conditions);
		this.potions = potions;
	}

	public @DefaultEmpty List<LootItemCondition> conditions() {
		return this.predicates;
	}

	@Override
	public ItemStack run(ItemStack stack, LootContext context) {
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(this.potions.getRandomElement(context.getRandom().nextLong())));
		return stack;
	}
}