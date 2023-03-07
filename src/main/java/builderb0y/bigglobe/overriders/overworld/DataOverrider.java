package builderb0y.bigglobe.overriders.overworld;

import java.lang.reflect.Method;

import com.google.common.collect.ObjectArrays;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.ColumnYScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.Wrappers.*;
import builderb0y.scripting.bytecode.CastingSupport.CastProvider;
import builderb0y.scripting.bytecode.CastingSupport.ConstantCaster;
import builderb0y.scripting.bytecode.CastingSupport.LookupCastProvider;
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
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface DataOverrider extends Script {

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

	public static double distanceToCircle(OverworldColumn column, StructureStartWrapper structure, double radius) {
		BlockBox box = structure.box();
		return distanceToCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			radius
		);
	}

	public static double distanceToCircle(OverworldColumn column, StructurePiece piece, double radius) {
		BlockBox box = piece.getBoundingBox();
		return distanceToCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			radius
		);
	}

	public static class Holder extends ScriptHolder<DataOverrider> implements DataOverrider {

		public Holder(ScriptParser<DataOverrider> parser) throws ScriptParsingException {
			super(
				parser
				.addCastProvider(DataOverrider.Environment.CAST_PROVIDER)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(new ColumnYScriptEnvironment(load("column", 2, type(OverworldColumn.class)), null, true))
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

		public static final CastProvider CAST_PROVIDER = (
			new LookupCastProvider()
			.append(TypeInfos.STRING, StructureEntry           .TYPE, true, new ConstantCaster(StructureEntry           .CONSTANT_FACTORY))
			.append(TypeInfos.STRING, StructureTagKey          .TYPE, true, new ConstantCaster(StructureTagKey          .CONSTANT_FACTORY))
			.append(TypeInfos.STRING, StructureTypeWrapper     .TYPE, true, new ConstantCaster(StructureTypeWrapper     .CONSTANT_FACTORY))
			.append(TypeInfos.STRING, StructurePieceTypeWrapper.TYPE, true, new ConstantCaster(StructurePieceTypeWrapper.CONSTANT_FACTORY))
		);

		public Environment() {
			this
			.addVariableLoad("rawGeneration", 3, TypeInfos.BOOLEAN)
			.addVariableLoad("structureStarts", 1, type(ScriptStructures.class))
			.addType("StructureStart",     StructureStartWrapper    .TYPE)
			.addType("StructurePiece",     StructurePieceWrapper    .TYPE)
			.addType("Structure",          StructureEntry           .TYPE)
			.addType("StructureTag",       StructureTagKey          .TYPE)
			.addType("StructureType",      StructureTypeWrapper     .TYPE)
			.addType("StructurePieceType", StructurePieceTypeWrapper.TYPE)
			.addFieldInvokes(StructureStartWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "structure", "pieces")
			.addFieldInvokeStatics(StructurePieceWrapper.class, "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "type", "hasPreferredTerrainHeight", "preferredTerrainHeight")
			.addMethodInvoke(StructureEntry.class, "isIn")
			.addFieldInvokes(StructureEntry.class, "type", "generationStep")
			;
			InsnTree columnLoader = load("column", 2, type(OverworldColumn.class));
			this.addMultiColumnFunctions(columnLoader, DataOverrider.class, "distanceToSquare", "distanceToCircle");
		}

		public void addColumnFunction(InsnTree column, MethodInfo method) {
			this.addFunction(method.name, (parser, name, arguments) -> {
				InsnTree[] prefixedArguments = ObjectArrays.concat(column, arguments);
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, prefixedArguments);
				return castArguments == null ? null : invokeStatic(method, castArguments);
			});
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
	}
}