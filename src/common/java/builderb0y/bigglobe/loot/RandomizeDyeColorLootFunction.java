package builderb0y.bigglobe.loot;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("conditions")
public class RandomizeDyeColorLootFunction extends LootItemConditionalFunction {

	public static final MapCodec<RandomizeDyeColorLootFunction> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RandomizeDyeColorLootFunction.class);

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return CODEC;
	}

	public RandomizeDyeColorLootFunction(List<LootItemCondition> conditions) {
		super(conditions);
	}

	@Override
	public ItemStack run(ItemStack stack, LootContext context) {
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(context.getRandom().nextInt() & 0xFFFFFF));
		return stack;
	}

	public @DefaultEmpty List<LootItemCondition> conditions() {
		return this.predicates;
	}
}