package builderb0y.bigglobe.overriders;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
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

	public static record Entry(Catcher script) implements Overrider {

		@Override
		public Type getOverriderType() {
			return Type.COLLISION;
		}
	}

	@Wrapper
	public static class Catcher extends ScriptCatcher<CollisionOverrider> implements CollisionOverrider {

		public Catcher(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(CollisionOverrider.class, this.usage, registry.parserFlags())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.create())
				.configureEnvironment(StructureScriptEnvironment.live())
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configure((ExpressionParser parser) -> {
					parser
					.environment
					.mutable()
					.addFieldGet(ScriptedStructure.Piece.class, "data")
					.addVariableLoad("currentStart", StructureStartWrapper.TYPE)
					.addVariableLoad("otherStart", StructureStartWrapper.TYPE)
					;
					registry.setupEnvironment(
						parser,
						new ExternalEnvironmentParams().withLookup(
							"columns",
							load("columns", type(ScriptedColumnLookup.class))
						)
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