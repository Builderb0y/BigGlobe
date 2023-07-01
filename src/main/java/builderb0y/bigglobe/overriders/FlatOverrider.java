package builderb0y.bigglobe.overriders;

import com.google.common.collect.ObjectArrays;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.FakeInstanceGetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface FlatOverrider extends Overrider {

	public static final MutableScriptEnvironment STRUCTURE_STARTS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariableLoad("structureStarts", 1, type(ScriptStructures.class))
	);

	public abstract void override(ScriptStructures structureStarts, WorldColumn column);

	public static VariableHandler createVariableFromStaticGetterAndSetter(Class<?> in, Class<? extends WorldColumn> columnClass, String getterName, String setterName) {
		MethodInfo getter = MethodInfo.getMethod(in, getterName);
		MethodInfo setter = MethodInfo.getMethod(in, setterName);
		InsnTree loadColumn = load("column", 2, type(columnClass));
		return new VariableHandler.Named(getter + " <-> " + setter, (ExpressionParser parser, String name) -> {
			return new FakeInstanceGetterInsnTree(getter, setter, loadColumn);
		});
	}

	public static FunctionHandler createColumnFunction(MethodInfo method, Class<? extends WorldColumn> columnClass) {
		InsnTree loadColumn = load("column", 2, type(columnClass));
		return new FunctionHandler.Named(method.toString(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			InsnTree[] prefixedArguments = ObjectArrays.concat(loadColumn, arguments);
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, prefixedArguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != prefixedArguments);
		});
	}

	public static FunctionHandler createColumnFunction(Class<?> in, Class<? extends WorldColumn> columnClass, String name) {
		return createColumnFunction(MethodInfo.getMethod(in, name), columnClass);
	}

	public static FunctionHandler createColumnFunction(Class<?> in, Class<? extends WorldColumn> columnClass, String name, Class<?> returnType, Class<?>... paramTypes) {
		return createColumnFunction(MethodInfo.findMethod(in, name, returnType, paramTypes), columnClass);
	}

	public static abstract class Holder<T_Overrider extends FlatOverrider> extends Overrider.Holder<T_Overrider> implements FlatOverrider {

		public Holder(ScriptParser<T_Overrider> parser, Class<? extends WorldColumn> columnClass) throws ScriptParsingException {
			super(
				parser
				.addEnvironment(STRUCTURE_STARTS_ENVIRONMENT)
				.addEnvironment(
					Overrider.createDistanceEnvironment(
						load("column", 2, type(columnClass))
					)
				)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						load("column", 2, type(columnClass)),
						null
					)
					.addXZ("x", "z")
					.build()
				)
			);
		}
	}
}