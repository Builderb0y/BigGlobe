package builderb0y.bigglobe.overriders;

import com.google.common.collect.ObjectArrays;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.invokers.GetterSetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface FlatOverrider extends Overrider {

	public static final MutableScriptEnvironment STRUCTURE_STARTS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariableLoad("structureStarts", type(ScriptStructures.class))
	);

	public abstract void override(ScriptStructures structureStarts, WorldColumn column);

	public static VariableHandler createVariableFromStaticGetterAndSetter(Class<?> in, Class<? extends WorldColumn> columnClass, String getterName, String setterName) {
		MethodInfo getter = MethodInfo.getMethod(in, getterName);
		MethodInfo setter = MethodInfo.getMethod(in, setterName);
		InsnTree loadColumn = load("column", type(columnClass));
		return new VariableHandler.Named(getter + " <-> " + setter, (ExpressionParser parser, String name) -> {
			return new GetterSetterInsnTree(loadColumn, getter, setter);
		});
	}

	public static VariableHandler createVariableFromField(Class<? extends WorldColumn> columnClass, String fieldName) {
		FieldInfo field = FieldInfo.getField(columnClass, fieldName);
		InsnTree loadColumn = load("column", type(columnClass));
		InsnTree getField = getField(loadColumn, field);
		return new VariableHandler.Named(field.toString(), (parser, name) -> getField);
	}

	public static FunctionHandler createColumnFunction(MethodInfo method, Class<? extends WorldColumn> columnClass) {
		InsnTree loadColumn = load("column", type(columnClass));
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

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, TemplateScriptParser<T_Overrider> parser) throws ScriptParsingException {
			super(
				usage,
				parser
				.addEnvironment(STRUCTURE_STARTS_ENVIRONMENT)
				.addEnvironment(
					Overrider.createDistanceEnvironment(
						load("column", type(WorldColumn.class))
					)
				)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						load("column", type(WorldColumn.class)),
						null
					)
					.addXZ("x", "z")
					.build()
				)
			);
		}
	}
}