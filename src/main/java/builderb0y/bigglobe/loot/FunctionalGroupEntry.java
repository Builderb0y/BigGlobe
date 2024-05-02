package builderb0y.bigglobe.loot;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootChoice;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultEmpty;

#if MC_VERSION >= MC_1_20_2
	import java.util.Arrays;
	import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
#else
	import com.google.gson.JsonDeserializationContext;
	import com.google.gson.JsonObject;
	import com.google.gson.JsonSerializationContext;
	import org.apache.commons.lang3.ArrayUtils;
	import net.minecraft.util.JsonHelper;
#endif

@AddPseudoField("conditions")
public class FunctionalGroupEntry extends LootPoolEntry {

	#if MC_VERSION >= MC_1_20_2
		public static final LootPoolEntryType SERIALIZER = new LootPoolEntryType(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(FunctionalGroupEntry.class) #if MC_VERSION >= MC_1_20_5 .fieldOf("value") #endif);
	#else
		public static final Serializer SERIALIZER = new Serializer();
	#endif

	public final LootPoolEntry[] children;
	public final LootFunction @DefaultEmpty [] functions;
	public final transient BiFunction<ItemStack, LootContext, ItemStack> compiledFunctions;

	public FunctionalGroupEntry(LootPoolEntry[] children, LootCondition[] conditions, LootFunction[] functions) {
		#if MC_VERSION >= MC_1_20_2
			super(Arrays.asList(conditions));
			this.compiledFunctions = LootFunctionTypes.join(Arrays.asList(functions));
		#else
			super(conditions);
			this.compiledFunctions = LootFunctionTypes.join(functions);
		#endif
		this.children = children;
		this.functions = functions;
	}

	public LootCondition @DefaultEmpty [] conditions() {
		#if MC_VERSION >= MC_1_20_2
			return this.conditions.toArray(new LootCondition[this.conditions.size()]);
		#else
			return this.conditions;
		#endif
	}

	@Override
	public LootPoolEntryType getType() {
		return BigGlobeLoot.FUNCTIONAL_GROUP;
	}

	@Override
	@SuppressWarnings("NonShortCircuitBooleanExpression")
	public boolean expand(LootContext context, Consumer<LootChoice> choiceConsumer) {
		boolean success = false;
		for (LootPoolEntry child : this.children) {
			success |= child.expand(context, choice -> {
				choiceConsumer.accept(new LootChoice() {

					@Override
					public int getWeight(float luck) {
						return choice.getWeight(luck);
					}

					@Override
					public void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
						choice.generateLoot(LootFunction.apply(FunctionalGroupEntry.this.compiledFunctions, lootConsumer, context), context);
					}
				});
			});
		}
		return success;
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);
		LootFunction[] functions = this.functions;
		for (int index = 0, length = functions.length; index < length; index++) {
			functions[index].validate(reporter.makeChild(".functions[" + index + ']'));
		}
	}

	#if MC_VERSION < MC_1_20_2
		public static class Serializer extends LootPoolEntry.Serializer<FunctionalGroupEntry> {

			@Override
			public void addEntryFields(JsonObject json, FunctionalGroupEntry entry, JsonSerializationContext context) {
				if (!ArrayUtils.isEmpty(entry.functions)) {
					json.add("functions", context.serialize(entry.functions));
				}
			}

			@Override
			public FunctionalGroupEntry fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
				LootPoolEntry[] children = JsonHelper.deserialize(json, "children", context, LootPoolEntry[].class);
				LootFunction[] functions = JsonHelper.deserialize(json, "functions", new LootFunction[0], context, LootFunction[].class);
				return new FunctionalGroupEntry(children, conditions, functions);
			}
		}
	#endif
}