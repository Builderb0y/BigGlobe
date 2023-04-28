package builderb0y.bigglobe.overriders.overworld;

import java.lang.reflect.Method;

import com.google.common.collect.ObjectArrays;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldDataOverrider extends Script {

	public abstract void override(ScriptStructures structureStarts, OverworldColumn column, boolean rawGeneration);

	public static double distanceToSquare(OverworldColumn column, double minX, double minZ, double maxX, double maxZ) {
		double offsetX = Interpolator.clamp(minX, maxX, column.x) - column.x;
		double offsetZ = Interpolator.clamp(minZ, maxZ, column.z) - column.z;
		return Math.sqrt(BigGlobeMath.squareD(offsetX, offsetZ));
	}

	public static double distanceToSquare(OverworldColumn column, StructureStartWrapper structure) {
		return distanceToSquare(column, structure.minX(), structure.minZ(), structure.maxX(), structure.maxZ());
	}

	public static double distanceToSquare(OverworldColumn column, StructurePiece piece) {
		BlockBox box = piece.getBoundingBox();
		return distanceToSquare(column, box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ());
	}

	public static double distanceToCircle(OverworldColumn column, double centerX, double centerZ, double radius) {
		return Math.max(Math.sqrt(BigGlobeMath.squareD(centerX - column.x, centerZ - column.z)) - radius, 0.0D);
	}

	public static double _distanceToCircle(OverworldColumn column, BlockBox box, double radius) {
		return distanceToCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			radius
		);
	}

	public static double _distanceToCircle(OverworldColumn column, BlockBox box) {
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

	public static double distanceToCircle(OverworldColumn column, StructureStartWrapper structure, double radius) {
		return _distanceToCircle(column, structure.box(), radius);
	}

	public static double distanceToCircle(OverworldColumn column, StructurePiece piece, double radius) {
		return _distanceToCircle(column, piece.getBoundingBox(), radius);
	}

	public static double distanceToCircle(OverworldColumn column, StructureStartWrapper structure) {
		return _distanceToCircle(column, structure.box());
	}

	public static double distanceToCircle(OverworldColumn column, StructurePiece piece) {
		return _distanceToCircle(column, piece.getBoundingBox());
	}

	public static class Holder extends ScriptHolder<OverworldDataOverrider> implements OverworldDataOverrider {

		public Holder(ScriptParser<OverworldDataOverrider> parser) throws ScriptParsingException {
			super(
				parser
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(
					ColumnScriptEnvironment.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						load("column", 2, type(OverworldColumn.class)),
						null
					)
					.addXZ("x", "z")
					.mutable
				)
				.parse()
			);
		}

		@Override
		public void override(ScriptStructures structureStarts, OverworldColumn column, boolean rawGeneration) {
			try {
				this.script.override(structureStarts, column, rawGeneration);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}

	public static class Environment extends MutableScriptEnvironment {

		public Environment() {
			this.addAll(StructureScriptEnvironment.INSTANCE);
		}

		public void addColumnFunction(InsnTree column, MethodInfo method) {
			this.addFunction(method.name, new FunctionHandler.Named(method + " (column argument implicit and automatic)", (parser, name, arguments) -> {
				InsnTree[] prefixedArguments = ObjectArrays.concat(column, arguments);
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, prefixedArguments);
				return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != prefixedArguments);
			}));
		}

		public void addColumnFunction(InsnTree column, Class<?> in, String name) {
			this.addColumnFunction(column, MethodInfo.getMethod(in, name));
		}

		public void addMultiColumnFunction(InsnTree column, Class<?> in, String name) {
			for (Method method : ReflectionData.forClass(in).getDeclaredMethods(name)) {
				this.addColumnFunction(column, MethodInfo.forMethod(method));
			}
		}

		public void addMultiColumnFunctions(InsnTree column, Class<?> in, String... names) {
			for (String name : names) {
				this.addMultiColumnFunction(column, in, name);
			}
		}

		public void addDistanceFunctions(InsnTree column) {
			this.addMultiColumnFunctions(column, OverworldDataOverrider.class, "distanceToSquare", "distanceToCircle");
		}
	}
}