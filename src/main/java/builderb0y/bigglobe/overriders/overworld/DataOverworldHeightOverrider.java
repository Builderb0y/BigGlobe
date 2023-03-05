package builderb0y.bigglobe.overriders.overworld;

import java.lang.reflect.Method;

import com.google.common.collect.ObjectArrays;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;

import builderb0y.autocodec.annotations.Wrapper;
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
import builderb0y.scripting.bytecode.FieldInfo;
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

public interface DataOverworldHeightOverrider extends Script {

	public void override(ScriptStructures structureStarts, OverworldColumn column, boolean rawGeneration);

	public static double excludeSquare(OverworldColumn column, double minX, double minZ, double maxX, double maxZ, double padding) {
		double offsetX = Interpolator.clamp(minX, maxX, column.x) - column.x;
		double offsetZ = Interpolator.clamp(minZ, maxZ, column.z) - column.z;
		double r2 = BigGlobeMath.squareD(offsetX, offsetZ);
		if (r2 < padding * padding) {
			return 1.0D - Interpolator.smooth(Math.sqrt(r2) / padding);
		}
		return 0.0D;
	}

	public static double excludeSquare(OverworldColumn column, StructureStartWrapper structure, double padding) {
		return excludeSquare(column, structure.minX(), structure.minZ(), structure.maxX(), structure.maxZ(), padding);
	}

	public static double excludeSquare(OverworldColumn column, StructurePiece piece, double padding) {
		BlockBox box = piece.getBoundingBox();
		return excludeSquare(column, box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ(), padding);
	}

	public static double excludeCircle(OverworldColumn column, double centerX, double centerZ, double radius, double padding) {
		double r = Math.sqrt(BigGlobeMath.squareD(centerX - column.x, centerZ - column.z));
		return 1.0D - Interpolator.unmixSmooth(radius, radius + padding, r);
	}

	public static double excludeCircle(OverworldColumn column, StructureStartWrapper structure, double padding) {
		BlockBox box = structure.box();
		return excludeCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			Math.min(
				box.getMaxX() - box.getMinX(),
				box.getMaxZ() - box.getMinZ()
			) * 0.5D,
			padding
		);
	}

	public static double excludeCircle(OverworldColumn column, StructurePiece piece, double padding) {
		BlockBox box = piece.getBoundingBox();
		return excludeCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			Math.min(
				box.getMaxX() - box.getMinX(),
				box.getMaxZ() - box.getMinZ()
			) * 0.5D,
			padding
		);
	}

	@Wrapper
	public static class Holder extends ScriptHolder<DataOverworldHeightOverrider> implements DataOverworldHeightOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(DataOverworldHeightOverrider.class, script)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(Environment.INSTANCE)
				.addCastProvider(Environment.CAST_PROVIDER)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				//todo: better environment which doesn't contain Y.
				.addEnvironment(new ColumnYScriptEnvironment(load("column", 2, type(OverworldColumn.class)), ldc(0.0D), true))
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

		public static final Environment INSTANCE = new Environment();

		public Environment() {
			InsnTree columnLoader = load("column", 2, type(OverworldColumn.class));
			this
			.addVariableLoad("rawGeneration", 3, TypeInfos.BOOLEAN)
			.addVariableLoad("structureStarts", 1, type(ScriptStructures.class))
			.addVariableRenamedGetField(columnLoader, "terrainY", FieldInfo.getField(OverworldColumn.class, "finalHeight"))
			.addVariableRenamedGetField(columnLoader, "snowY", FieldInfo.getField(OverworldColumn.class, "snowHeight"))
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
			for (Method method : ReflectionData.forClass(DataOverworldHeightOverrider.class).getDeclaredMethods("excludeSquare")) {
				this.addColumnFunction(columnLoader, MethodInfo.forMethod(method));
			}
			for (Method method : ReflectionData.forClass(DataOverworldHeightOverrider.class).getDeclaredMethods("excludeCircle")) {
				this.addColumnFunction(columnLoader, MethodInfo.forMethod(method));
			}
		}

		public void addColumnFunction(InsnTree column, MethodInfo method) {
			this.addFunction(method.name, (parser, name, arguments) -> {
				InsnTree[] prefixedArguments = ObjectArrays.concat(column, arguments);
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_NULL, prefixedArguments);
				return castArguments == null ? null : invokeStatic(method, castArguments);
			});
		}
	}
}