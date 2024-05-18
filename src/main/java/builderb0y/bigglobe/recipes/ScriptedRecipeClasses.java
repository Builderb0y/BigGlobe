package builderb0y.bigglobe.recipes;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.CraftingGridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.ItemScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.CraftingGrid;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedRecipeClasses {

	public static final AutoCoder<CraftingRecipeCategory> CATEGORY_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(CraftingRecipeCategory.CODEC, false);

	/** workaround for only some MC versions needing "value": {} */
	public static record ScriptedRecipeData(
		@UseCoder(
			name = "CATEGORY_CODER",
			in = ScriptedRecipeClasses.class,
			usage = MemberUsage.FIELD_CONTAINS_HANDLER
		)
		CraftingRecipeCategory category,
		int width,
		int height,
		CraftingMatchesScript.Holder matches,
		CraftingOutputScript.Holder output,
		CraftingRemainderScript.@VerifyNullable Holder remainder
	) {}

	public static interface CraftingMatchesScript extends Script {

		public abstract boolean matches(CraftingGrid input);

		@Wrapper
		public static class Holder extends ScriptHolder<CraftingMatchesScript> implements CraftingMatchesScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(CraftingMatchesScript.class, this.usage)
					.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
					.configureEnvironment(NbtScriptEnvironment.createMutable())
					.addEnvironment(ItemScriptEnvironment.INSTANCE)
					.addEnvironment(CraftingGridScriptEnvironment.INSTANCE)
					.configureEnvironment((MutableScriptEnvironment environment) ->
						environment.addVariableLoad("input", type(CraftingGrid.class))
					)
					.parse(new ScriptClassLoader())
				);
			}

			@Override
			public boolean requiresColumns() {
				return false;
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

		public abstract ItemStack output(CraftingGrid input);

		@Wrapper
		public static class Holder extends ScriptHolder<CraftingOutputScript> implements CraftingOutputScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(CraftingOutputScript.class, this.usage)
					.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
					.configureEnvironment(NbtScriptEnvironment.createMutable())
					.addEnvironment(ItemScriptEnvironment.INSTANCE)
					.addEnvironment(CraftingGridScriptEnvironment.INSTANCE)
					.configureEnvironment((MutableScriptEnvironment environment) ->
						environment.addVariableLoad("input", type(CraftingGrid.class))
					)
					.parse(new ScriptClassLoader())
				);
			}

			@Override
			public boolean requiresColumns() {
				return false;
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

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(CraftingRemainderScript.class, this.usage)
					.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
					.configureEnvironment(NbtScriptEnvironment.createMutable())
					.addEnvironment(ItemScriptEnvironment.INSTANCE)
					.addEnvironment(CraftingGridScriptEnvironment.INSTANCE)
					.configureEnvironment((MutableScriptEnvironment environment) ->
						environment
						.addVariableLoad("input",  type(CraftingGrid.class))
						.addVariableLoad("output", type(CraftingGrid.class))
					)
					.parse(new ScriptClassLoader())
				);
			}

			@Override
			public boolean requiresColumns() {
				return false;
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