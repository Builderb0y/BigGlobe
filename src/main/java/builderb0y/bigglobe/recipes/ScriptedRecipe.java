package builderb0y.bigglobe.recipes;

import java.util.stream.IntStream;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.CraftingGridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.ItemScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.CraftingGrid;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

#if MC_VERSION == MC_1_19_2
	import net.minecraft.inventory.CraftingInventory;
#elif MC_VERSION == MC_1_19_4
	import net.minecraft.inventory.CraftingInventory;
	import net.minecraft.recipe.book.CraftingRecipeCategory;
	import net.minecraft.registry.DynamicRegistryManager;
#elif MC_VERSION == MC_1_20_1
	import net.minecraft.inventory.RecipeInputInventory;
	import net.minecraft.recipe.book.CraftingRecipeCategory;
	import net.minecraft.registry.DynamicRegistryManager;
#endif

@AddPseudoField("id")
#if MC_VERSION >= MC_1_19_4
@AddPseudoField("category")
#endif
public class ScriptedRecipe extends SpecialCraftingRecipe {

	#if MC_VERSION >= MC_1_19_4
		public static final AutoCoder<CraftingRecipeCategory> CATEGORY_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(CraftingRecipeCategory.CODEC, false);
	#endif

	public final int width, height;
	public final CraftingMatchesScript.Holder matches;
	public final CraftingOutputScript.Holder output;
	public final CraftingRemainderScript.@VerifyNullable Holder remainder;

	public ScriptedRecipe(
		Identifier id,
		#if MC_VERSION >= MC_1_19_4 CraftingRecipeCategory category, #endif
		int width,
		int height,
		CraftingMatchesScript.Holder matches,
		CraftingOutputScript.Holder output,
		CraftingRemainderScript.@VerifyNullable Holder remainder
	) {
		super(id #if MC_VERSION >= MC_1_19_4 , category #endif);
		this.width     = width;
		this.height    = height;
		this.matches   = matches;
		this.output    = output;
		this.remainder = remainder;
	}

	public Identifier id() {
		return this.getId();
	}

	#if MC_VERSION >= MC_1_19_4
	public @UseCoder(name = "CATEGORY_CODER", in = ScriptedRecipe.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) CraftingRecipeCategory category() {
		return this.getCategory();
	}
	#endif

	@Override
	#if MC_VERSION == MC_1_19_2 || MC_VERSION == MC_1_19_4
		public boolean matches(CraftingInventory inventory, World world) {
	#elif MC_VERSION == MC_1_20_1
		public boolean matches(RecipeInputInventory inventory, World world) {
	#else
		#error "check if minecraft changed the recipe methods again or not."
	#endif
		return this.matches.matches(new CraftingGrid(
			IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	#if MC_VERSION == MC_1_19_2
		public ItemStack craft(CraftingInventory inventory) {
	#elif MC_VERSION == MC_1_19_4
		public ItemStack craft(CraftingInventory inventory, DynamicRegistryManager dynamicRegistryManager) {
	#elif MC_VERSION == MC_1_20_1
		public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager dynamicRegistryManager) {
	#else
		#error "check if minecraft changed the recipe methods again or not."
	#endif
		return this.output.output(new CraftingGrid(
			IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	#if MC_VERSION == MC_1_19_2 || MC_VERSION == MC_1_19_4
		public DefaultedList<ItemStack> getRemainder(CraftingInventory inventory) {
	#elif MC_VERSION == MC_1_20_1
		public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
	#else
		#error "check if minecraft changed the recipe methods again or not."
	#endif
		if (this.remainder != null) {
			CraftingGrid input = new CraftingGrid(
				IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
				inventory.getWidth(),
				inventory.getHeight(),
				false
			);
			CraftingGrid output = new CraftingGrid(
				IntStream.range(0, inventory.size()).mapToObj(index -> ItemStack.EMPTY),
				inventory.getWidth(),
				inventory.getHeight(),
				true
			);
			this.remainder.remainder(input, output);
			return new DefaultedList<>(output, ItemStack.EMPTY) {};
		}
		else {
			return super.getRemainder(inventory);
		}
	}

	@Override
	public boolean fits(int width, int height) {
		return width >= this.width && height >= this.height;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return BigGlobeRecipeSerializers.SCRIPTED;
	}

	public static interface CraftingMatchesScript extends Script {

		public abstract boolean matches(CraftingGrid grid);

		@Wrapper
		public static class Holder extends ScriptHolder<CraftingMatchesScript> implements CraftingMatchesScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(
					usage,
					new TemplateScriptParser<>(
						CraftingMatchesScript.class,
						usage
					)
					.addEnvironment(JavaUtilScriptEnvironment.ALL)
					.addEnvironment(NbtScriptEnvironment.INSTANCE)
					.addEnvironment(ItemScriptEnvironment.INSTANCE)
					.addEnvironment(CraftingGridScriptEnvironment.INSTANCE)
					.configureEnvironment(environment -> environment.addVariableLoad("input", 1, type(CraftingGrid.class)))
					.parse()
				);
			}

			@Override
			public boolean matches(CraftingGrid grid) {
				try {
					return this.script.matches(grid);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface CraftingOutputScript extends Script {

		public abstract ItemStack output(CraftingGrid grid);

		@Wrapper
		public static class Holder extends ScriptHolder<CraftingOutputScript> implements CraftingOutputScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(
					usage,
					new TemplateScriptParser<>(
						CraftingOutputScript.class,
						usage
					)
					.addEnvironment(JavaUtilScriptEnvironment.ALL)
					.addEnvironment(NbtScriptEnvironment.INSTANCE)
					.addEnvironment(ItemScriptEnvironment.INSTANCE)
					.addEnvironment(CraftingGridScriptEnvironment.INSTANCE)
					.configureEnvironment(environment -> environment.addVariableLoad("input", 1, type(CraftingGrid.class)))
					.parse()
				);
			}

			@Override
			public ItemStack output(CraftingGrid grid) {
				try {
					ItemStack stack = this.script.output(grid);
					return stack != null ? stack : ItemStack.EMPTY;
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return ItemStack.EMPTY;
				}
			}
		}
	}

	public static interface CraftingRemainderScript extends Script {

		public abstract void remainder(CraftingGrid input, CraftingGrid output);

		@Wrapper
		public static class Holder extends ScriptHolder<CraftingRemainderScript> implements CraftingRemainderScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(
					usage,
					new TemplateScriptParser<>(
						CraftingRemainderScript.class,
						usage
					)
					.addEnvironment(JavaUtilScriptEnvironment.ALL)
					.addEnvironment(NbtScriptEnvironment.INSTANCE)
					.addEnvironment(ItemScriptEnvironment.INSTANCE)
					.addEnvironment(CraftingGridScriptEnvironment.INSTANCE)
					.configureEnvironment(environment -> {
						environment
						.addVariableLoad("input", 1, type(CraftingGrid.class))
						.addVariableLoad("output", 2, type(CraftingGrid.class))
						;
					})
					.parse()
				);
			}

			@Override
			public void remainder(CraftingGrid input, CraftingGrid output) {
				try {
					this.script.remainder(input, output);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
				}
			}
		}
	}
}