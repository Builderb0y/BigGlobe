package builderb0y.bigglobe.overriders.end;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface EndHeightOverrider extends EndFlatOverrider {

	public static final MutableScriptEnvironment Y_LEVELS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("mountainCenterY",   FlatOverrider.createVariableFromStaticGetterAndSetter(EndHeightOverrider.class, EndColumn.class, "getMountainCenterY",   "setMountainCenterY"  ))
		.addVariable("mountainThickness", FlatOverrider.createVariableFromStaticGetterAndSetter(EndHeightOverrider.class, EndColumn.class, "getMountainThickness", "setMountainThickness"))
		.addVariable("mountainMinY",      FlatOverrider.createVariableFromStaticGetterAndSetter(EndHeightOverrider.class, EndColumn.class, "getMountainMinY",      "setMountainMinY"     ))
		.addVariable("mountainMaxY",      FlatOverrider.createVariableFromStaticGetterAndSetter(EndHeightOverrider.class, EndColumn.class, "getMountainMaxY",      "setMountainMaxY"     ))
	);

	public static double getMountainCenterY(EndColumn column) {
		return column.mountainCenterY;
	}

	public static void setMountainCenterY(EndColumn column, double y) {
		column.mountainCenterY = y;
	}

	public static double getMountainThickness(EndColumn column) {
		return column.mountainThickness;
	}

	public static void setMountainThickness(EndColumn column, double y) {
		column.mountainThickness = y;
	}

	public static double getMountainMinY(EndColumn column) {
		return column.mountainCenterY - column.mountainThickness;
	}

	public static double getMountainMaxY(EndColumn column) {
		return column.mountainCenterY + column.mountainThickness;
	}

	public static void setMountainMinY(EndColumn column, double minY) {
		double maxY = column.mountainCenterY + column.mountainThickness;
		column.mountainCenterY = (maxY + minY) * 0.5D;
		column.mountainThickness = (maxY - minY) * 0.5D;
	}

	public static void setMountainMaxY(EndColumn column, double maxY) {
		double minY = column.mountainCenterY - column.mountainThickness;
		column.mountainCenterY = (maxY + minY) * 0.5D;
		column.mountainThickness = (maxY - minY) * 0.5D;
	}

	@Wrapper
	public static class Holder extends EndFlatOverrider.Holder<EndHeightOverrider> implements EndHeightOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(EndHeightOverrider.class, usage)
				.addEnvironment(Y_LEVELS_ENVIRONMENT)
			);
		}
	}
}