package builderb0y.bigglobe.overriders;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface CollisionOverrider extends ColumnScript {

	public abstract int override(
		ScriptedColumnLookup columns,
		StructureStartWrapper currentStart,
		StructureStartWrapper otherStart
	);

	public static record Entry(Holder script) implements Overrider {

		@Override
		public Type getOverriderType() {
			return Type.COLLISION;
		}
	}

	@Wrapper
	public static class Holder extends ScriptHolder<CollisionOverrider> implements CollisionOverrider {

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(CollisionOverrider.class, this.usage)
				.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(RandomScriptEnvironment.BASE)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.create())
				.configureEnvironment(MinecraftScriptEnvironment.create())
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					LoadInsnTree loadLookup = load("columns", type(ScriptedColumnLookup.class));
					registry.setupExternalEnvironment(
						environment
						.addFieldGet(ScriptedStructure.Piece.class, "data")
						.addVariableLoad("currentStart", StructureStartWrapper.TYPE)
						.addVariableLoad("otherStart", StructureStartWrapper.TYPE)
						.addVariable("hints", Handlers.builder(ScriptedColumnLookup.HINTS).addImplicitArgument(loadLookup).buildVariable())
						.configure(ScriptedColumn.hintsEnvironment()),
						new ExternalEnvironmentParams().withLookup(loadLookup)
					);
				})
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public int override(
			ScriptedColumnLookup columns,
			StructureStartWrapper currentStart,
			StructureStartWrapper otherStart
		) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				return this.script.override(columns, currentStart, otherStart);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return 0;
			}
			finally {
				manager.used = used;
			}
		}
	}
}