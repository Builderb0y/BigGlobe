package builderb0y.bigglobe.commands;

import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.Wrappers.WorldWrapper;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.GetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.UncheckedReflection;
import builderb0y.scripting.parsing.*;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface CommandScript extends Script {

	public abstract Object evaluate(WorldWrapper world, WorldColumn column, double x, double y, double z);

	public static class Parser extends ScriptParser<CommandScript> {

		public static final Method IMPLEMENTING_METHOD = UncheckedReflection.getDeclaredMethod(
			CommandScript.class,
			"evaluate",
			WorldWrapper.class,
			WorldColumn.class,
			double.class,
			double.class,
			double.class
		);

		public Parser(String input) {
			super(CommandScript.class, IMPLEMENTING_METHOD, input);
			this
			.addEnvironment(JavaUtilScriptEnvironment.INSTANCE)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addCastProvider(MinecraftScriptEnvironment.CAST_PROVIDER)
			.addEnvironment(new MinecraftScriptEnvironment(
				new LoadInsnTree(new VarInfo("world", 1, WorldWrapper.TYPE))
			))
			.addEnvironment(NBTScriptEnvironment.INSTANCE)
			.addCastProvider(NBTScriptEnvironment.NBT_CASTS)
			.addEnvironment(new ColumnYScriptEnvironment(
				new LoadInsnTree(new VarInfo("column", 2, type(WorldColumn.class))),
				new LoadInsnTree(new VarInfo("y", 5, TypeInfos.DOUBLE)),
				false
			))
			.addEnvironment(
				new MutableScriptEnvironment()
				.addParameter("x", 3, TypeInfos.DOUBLE)
				.addParameter("y", 5, TypeInfos.DOUBLE)
				.addParameter("z", 7, TypeInfos.DOUBLE)
			)
			.addEnvironment(new RandomScriptEnvironment(
				new GetFieldInsnTree(
					new LoadInsnTree(new VarInfo("world", 1, WorldWrapper.TYPE)),
					new FieldInfo(
						Opcodes.ACC_PUBLIC,
						WorldWrapper.TYPE,
						"permuter",
						type(Permuter.class)
					)
				)
			));
		}

		@Override
		public InsnTree createReturn(InsnTree value) {
			return return_(value.cast(this, TypeInfos.OBJECT, CastMode.EXPLICIT_THROW));
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
		public Object evaluate(WorldWrapper world, WorldColumn column, double x, double y, double z) {
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