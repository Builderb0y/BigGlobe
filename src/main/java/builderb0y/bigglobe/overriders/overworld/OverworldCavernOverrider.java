package builderb0y.bigglobe.overriders.overworld;

import java.lang.reflect.Method;

import net.minecraft.structure.StructurePiece;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldCavernOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment CAVERN_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("cavernCenterY", FlatOverrider.createVariableFromField(OverworldColumn.class, "cavernCenter"))
		.addVariable("cavernThicknessSquared", FlatOverrider.createVariableFromField(OverworldColumn.class, "cavernThicknessSquared"))
		.addVariable("exclusionMultiplier", invokeStatic(MethodInfo.getMethod(OverworldCavernOverrider.class, "getExclusionMultiplier"), load("column", type(OverworldColumn.class))))
		.configure((MutableScriptEnvironment environment) -> {
			for (Method method : ReflectionData.forClass(OverworldCavernOverrider.class).getDeclaredMethods("getOverlap")) {
				environment.addFunction("getOverlap", FlatOverrider.createColumnFunction(MethodInfo.forMethod(method), OverworldColumn.class));
			}
		})
		.addFunction("exclude", FlatOverrider.createColumnFunction(OverworldCavernOverrider.class, OverworldColumn.class, "exclude"))
	);

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

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public Class<OverworldCavernOverrider> getScriptClass() {
			return OverworldCavernOverrider.class;
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return super.setupEnvironment(environment).addAll(CAVERN_ENVIRONMENT);
		}
	}
}