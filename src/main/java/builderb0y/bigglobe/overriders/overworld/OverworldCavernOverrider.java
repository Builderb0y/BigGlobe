package builderb0y.bigglobe.overriders.overworld;

import java.lang.reflect.Method;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldCavernOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment CAVERN_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("cavernCenterY", FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldCavernOverrider.class, OverworldColumn.class, "getCavernCenterY", "setCavernCenterY"))
		.addVariable("cavernThicknessSquared", FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldCavernOverrider.class, OverworldColumn.class, "getCavernThicknessSquared", "setCavernThicknessSquared"))
		.addVariable("exclusionMultiplier", invokeStatic(MethodInfo.getMethod(OverworldCavernOverrider.class, "getExclusionMultiplier"), load("column", 2, type(OverworldColumn.class))))
		.configure((MutableScriptEnvironment environment) -> {
			for (Method method : ReflectionData.forClass(OverworldCavernOverrider.class).getDeclaredMethods("getOverlap")) {
				environment.addFunction("getOverlap", FlatOverrider.createColumnFunction(MethodInfo.forMethod(method), OverworldColumn.class));
			}
		})
		.addFunction("exclude", FlatOverrider.createColumnFunction(OverworldCavernOverrider.class, OverworldColumn.class, "exclude"))
	);

	public static double getCavernCenterY(OverworldColumn column) {
		return column.cavernCenter;
	}

	public static void setCavernCenterY(OverworldColumn column, double center) {
		column.cavernCenter = center;
	}

	public static double getCavernThicknessSquared(OverworldColumn column) {
		return column.cavernThicknessSquared;
	}

	public static void setCavernThicknessSquared(OverworldColumn column, double thicknessSquared) {
		column.cavernThicknessSquared = thicknessSquared;
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

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldCavernOverrider> implements OverworldCavernOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldCavernOverrider.class, script)
				.addEnvironment(CAVERN_ENVIRONMENT)
			);
		}
	}
}