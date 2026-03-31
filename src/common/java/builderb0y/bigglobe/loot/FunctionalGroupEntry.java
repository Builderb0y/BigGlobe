package builderb0y.bigglobe.loot;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("conditions")
public class FunctionalGroupEntry extends LootPoolEntryContainer {

	public static final MapCodec<FunctionalGroupEntry> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(FunctionalGroupEntry.class);

	@Override
	public MapCodec<? extends LootPoolEntryContainer> codec() {
		return CODEC;
	}

	public final LootPoolEntryContainer[] children;
	public final @DefaultEmpty List<LootItemFunction> functions;
	public final transient BiFunction<ItemStack, LootContext, ItemStack> compiledFunctions;

	public FunctionalGroupEntry(LootPoolEntryContainer[] children, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
		super(conditions);
		this.compiledFunctions = LootItemFunctions.compose(functions);
		this.children = children;
		this.functions = functions;
	}

	public @DefaultEmpty List<LootItemCondition> conditions() {
		return this.conditions;
	}

	@Override
	@SuppressWarnings("NonShortCircuitBooleanExpression")
	public boolean expand(LootContext context, Consumer<LootPoolEntry> choiceConsumer) {
		boolean success = false;
		for (LootPoolEntryContainer child : this.children) {
			success |= child.expand(
				context,
				(LootPoolEntry choice) -> {
					choiceConsumer.accept(new LootPoolEntry() {

						@Override
						public int getWeight(float luck) {
							return choice.getWeight(luck);
						}

						@Override
						public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext context) {
							choice.createItemStack(LootItemFunction.decorate(FunctionalGroupEntry.this.compiledFunctions, lootConsumer, context), context);
						}
					});
				}
			);
		}
		return success;
	}

	@Override
	public void validate(ValidationContext reporter) {
		super.validate(reporter);
		List<LootItemFunction> functions = this.functions;
		for (int index = 0, size = functions.size(); index < size; index++) {
			final int index_ = index; //lambdas -_-
			functions.get(index).validate(reporter.forChild(() -> ".functions[" + index_ + ']'));
		}
	}
}