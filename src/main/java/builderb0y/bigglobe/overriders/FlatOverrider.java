package builderb0y.bigglobe.overriders;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import com.google.common.collect.ObjectArrays;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.FakeInstanceGetterInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface FlatOverrider extends Script {

	public static Consumer<MutableScriptEnvironment> structureDistanceEnvironment(Class<? extends WorldColumn> columnClass) {
		return environment -> {
			environment
			.addAll(StructureScriptEnvironment.INSTANCE)
			.addVariableLoad("structureStarts", 1, type(ScriptStructures.class));
			for (String name : new String[] { "distanceToSquare", "distanceToCircle"}) {
				for (Method method : ReflectionData.forClass(FlatOverrider.class).getDeclaredMethods(name)) {
					environment.addFunction(name, createColumnFunction(MethodInfo.forMethod(method), columnClass));
				}
			}
		};
	}

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

	public static double distanceToSquare(WorldColumn column, double minX, double minZ, double maxX, double maxZ) {
		double offsetX = Interpolator.clamp(minX, maxX, column.x) - column.x;
		double offsetZ = Interpolator.clamp(minZ, maxZ, column.z) - column.z;
		return Math.sqrt(BigGlobeMath.squareD(offsetX, offsetZ));
	}

	public static double distanceToSquare(WorldColumn column, StructureStartWrapper structure) {
		return distanceToSquare(column, structure.minX(), structure.minZ(), structure.maxX(), structure.maxZ());
	}

	public static double distanceToSquare(WorldColumn column, StructurePiece piece) {
		BlockBox box = piece.getBoundingBox();
		return distanceToSquare(column, box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ());
	}

	public static double distanceToCircle(WorldColumn column, double centerX, double centerZ, double radius) {
		return Math.max(Math.sqrt(BigGlobeMath.squareD(centerX - column.x, centerZ - column.z)) - radius, 0.0D);
	}

	public static double _distanceToCircle(WorldColumn column, BlockBox box, double radius) {
		return distanceToCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			radius
		);
	}

	public static double _distanceToCircle(WorldColumn column, BlockBox box) {
		return _distanceToCircle(
			column,
			box,
			Math.min(
				box.getMaxX() - box.getMinX(),
				box.getMaxZ() - box.getMinZ()
			)
			* 0.5D
		);
	}

	public static double distanceToCircle(WorldColumn column, StructureStartWrapper structure, double radius) {
		return _distanceToCircle(column, structure.box(), radius);
	}

	public static double distanceToCircle(WorldColumn column, StructurePiece piece, double radius) {
		return _distanceToCircle(column, piece.getBoundingBox(), radius);
	}

	public static double distanceToCircle(WorldColumn column, StructureStartWrapper structure) {
		return _distanceToCircle(column, structure.box());
	}

	public static double distanceToCircle(WorldColumn column, StructurePiece piece) {
		return _distanceToCircle(column, piece.getBoundingBox());
	}

	public static abstract class Holder<T_Overrider extends FlatOverrider> extends ScriptHolder<T_Overrider> implements FlatOverrider {

		public Holder(ScriptParser<T_Overrider> parser, Class<? extends WorldColumn> columnClass) throws ScriptParsingException {
			super(
				parser
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.configureEnvironment(FlatOverrider.structureDistanceEnvironment(columnClass))
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						load("column", 2, type(columnClass)),
						null
					)
					.addXZ("x", "z")
					.build()
				)
				.parse()
			);
		}
	}
}