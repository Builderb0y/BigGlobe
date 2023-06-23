package builderb0y.bigglobe.overriders.overworld;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class OverworldCavernOverrider {

	@Wrapper
	public static class Holder extends OverworldDataOverrider.Holder {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldDataOverrider.class, script)
				.addEnvironment(OverworldCavernOverrider.Environment.INSTANCE)
			);
		}
	}

	public static class Environment extends OverworldDataOverrider.Environment {

		public static final Environment INSTANCE = new Environment();

		public Environment() {
			super();

			this
			.addVariableLoad("structureStarts", 1, type(ScriptStructures.class))
			;
			InsnTree columnLoader = load("column", 2, type(OverworldColumn.class));
			this.addDistanceFunctions(columnLoader);
			this
			.addVariableRenamedGetField(columnLoader, "cavernCenterY", OverworldColumn.class, "cavernCenter")
			.addVariableRenamedGetField(columnLoader, "cavernThicknessSquared", OverworldColumn.class, "cavernThicknessSquared")
			;
			this.addVariable("exclusionMultiplier", invokeStatic(MethodInfo.getMethod(Environment.class, "getExclusionMultiplier"), columnLoader));
			this.addMultiColumnFunction(columnLoader, Environment.class, "getOverlap");
			this.addColumnFunction(columnLoader, Environment.class, "exclude");
		}

		public static double getExclusionMultiplier(OverworldColumn column) {
			return column.getCavernThicknessSquared();
		}

		public static void exclude(OverworldColumn column, double amount) {
			column.cavernThicknessSquared -= amount * getExclusionMultiplier(column);
		}

		public static double getOverlap(OverworldColumn column, double minY, double maxY, double padding) {
			double cavernCenter = column.getCavernCenter();
			double thickness = column.getCavernThickness();
			double radius = (maxY - minY) * 0.5D;
			double center = (maxY + minY) * 0.5D;
			return Math.max(radius + thickness + padding - Math.abs(center - cavernCenter), 0.0D) / thickness;
		}

		public static double getOverlap(OverworldColumn column, StructureStartWrapper start, double padding) {
			return getOverlap(column, start.minY(), start.maxY(), padding);
		}

		public static double getOverlap(OverworldColumn column, StructurePiece piece, double padding) {
			return getOverlap(column, piece.getBoundingBox().getMinY(), piece.getBoundingBox().getMaxY(), padding);
		}
	}
}