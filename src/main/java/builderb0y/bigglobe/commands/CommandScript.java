package builderb0y.bigglobe.commands;

import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface CommandScript extends Script {

	public abstract Object evaluate(WorldWrapper world, WorldColumn column, int x, int y, int z);

	public static class Parser extends ScriptParser<CommandScript> {

		public static final Method IMPLEMENTING_METHOD = ReflectionData.forClass(CommandScript.class).getDeclaredMethod("evaluate");
		public static final InsnTree LOAD_RANDOM = getField(
			load("world", 1, WorldWrapper.TYPE),
			FieldInfo.getField(WorldWrapper.class, "permuter")
		);

		public Parser(String input) {
			super(CommandScript.class, IMPLEMENTING_METHOD, input);
			this
			.addEnvironment(JavaUtilScriptEnvironment.ALL)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(MinecraftScriptEnvironment.createWithWorld(
				load("world", 1, WorldWrapper.TYPE)
			))
			.addEnvironment(NbtScriptEnvironment.INSTANCE)
			.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
			.addEnvironment(
				ColumnScriptEnvironment.createVariableXYZ(
					ColumnValue.REGISTRY,
					load("column", 2, type(WorldColumn.class))
				)
				.mutable
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("originX", 3, TypeInfos.INT)
				.addVariableLoad("originY", 4, TypeInfos.INT)
				.addVariableLoad("originZ", 5, TypeInfos.INT)
			)
			.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
			.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE);
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
				this.script = this.parser.toScript();
				this.parser = null; //free for GC.
			}
			catch (ScriptParsingException exception) {
				throw new RuntimeException(exception);
			}
			return this.script;
		}

		@Override
		public Object evaluate(WorldWrapper world, WorldColumn column, int x, int y, int z) {
			try {
				return this.getScript().evaluate(world, column, x, y, z);
			}
			catch (Throwable throwable) {
				ScriptLogger.LOGGER.error("Caught exception from CommandScript:", throwable);
				ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
				return null;
			}
		}

		@Override
		public String getSource() {
			return this.getScript().getSource();
		}
	}
}