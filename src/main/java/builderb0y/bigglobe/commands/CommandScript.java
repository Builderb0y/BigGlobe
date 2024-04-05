package builderb0y.bigglobe.commands;

import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface CommandScript extends Script {

	public abstract Object evaluate(WorldWrapper world, WorldColumn column, int originX, int originY, int originZ);

	public static class Parser extends ScriptParser<CommandScript> {

		public static final Method IMPLEMENTING_METHOD = ReflectionData.forClass(CommandScript.class).getDeclaredMethod("evaluate");
		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public Parser(String input) {
			super(CommandScript.class, IMPLEMENTING_METHOD, input, (String)(null));
			this
			.addEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
			.addEnvironment(SymmetryScriptEnvironment.create(WORLD.random))
			.addEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
			.addEnvironment(NbtScriptEnvironment.INSTANCE)
			.addEnvironment(WoodPaletteScriptEnvironment.create(WORLD.random))
			.addEnvironment(
				ColumnScriptEnvironmentBuilder.createVariableXYZ(
					ColumnValue.REGISTRY,
					load("column", type(WorldColumn.class))
				)
				.build()
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("originX", TypeInfos.INT)
				.addVariableLoad("originY", TypeInfos.INT)
				.addVariableLoad("originZ", TypeInfos.INT)
			)
			.addEnvironment(RandomScriptEnvironment.create(WORLD.random))
			.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
			.addEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf));
		}

		@Override
		public InsnTree createReturn(InsnTree value) {
			if (value.getTypeInfo().isVoid()) return return_(seq(value, ldc(null, TypeInfos.OBJECT)));
			else return return_(value.cast(this, TypeInfos.OBJECT, CastMode.EXPLICIT_THROW));
		}
	}

	public static class LazyCommandScript implements CommandScript {

		public Parser parser;
		public @Nullable CommandScript script;

		public LazyCommandScript(String script) throws ScriptParsingException {
			this.parser = new Parser(script);
			this.parser.toBytecode();
		}

		public CommandScript getScript() {
			if (this.script == null) try {
				this.script = this.parser.toScript(new ScriptClassLoader());
			}
			catch (ScriptParsingException exception) {
				throw new RuntimeException(exception);
			}
			return this.script;
		}

		@Override
		public Object evaluate(WorldWrapper world, WorldColumn column, int originX, int originY, int originZ) {
			try {
				return this.getScript().evaluate(world, column, originX, originY, originZ);
			}
			catch (Throwable throwable) {
				ScriptLogger.LOGGER.error("Caught exception from CommandScript:", throwable);
				ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
				return throwable;
			}
		}

		@Override
		public String getSource() {
			return this.getScript().getSource();
		}

		@Override
		public @Nullable String getDebugName() {
			return this.getScript().getDebugName();
		}
	}
}