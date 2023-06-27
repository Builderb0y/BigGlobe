package builderb0y.bigglobe.overriders;

import java.lang.reflect.Method;

import com.google.common.collect.ObjectArrays;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface Overrider extends Script {

	public static MutableScriptEnvironment createDistanceEnvironment(InsnTree loadColumn) {
		MutableScriptEnvironment environment = new MutableScriptEnvironment();
		for (String name : new String[] { "distanceToSquare", "distanceToCircle" }) {
			for (Method method : ReflectionData.forClass(Overrider.class).getDeclaredMethods(name)) {
				environment.addFunction(name, createColumnFunction(MethodInfo.forMethod(method), loadColumn));
			}
		}
		return environment;
	}

	public static FunctionHandler createColumnFunction(MethodInfo method, InsnTree loadColumn) {
		return new FunctionHandler.Named(method.toString(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
			InsnTree[] prefixedArguments = ObjectArrays.concat(loadColumn, arguments);
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, prefixedArguments);
			return castArguments == null ? null : new CastResult(invokeStatic(method, castArguments), castArguments != prefixedArguments);
		});
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

	public static class Holder<T_Overrider extends Overrider> extends ScriptHolder<T_Overrider> implements Overrider {

		public Holder(ScriptParser<T_Overrider> parser) throws ScriptParsingException {
			super(
				parser
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.parse()
			);
		}
	}
}