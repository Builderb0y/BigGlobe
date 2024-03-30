package builderb0y.bigglobe.overriders.end;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptUsage;

public interface EndHeightOverrider extends EndFlatOverrider {

	public static final MutableScriptEnvironment Y_LEVELS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("mountainCenterY",   FlatOverrider.createVariableFromField(EndColumn.class, "mountainCenterY"))
		.addVariable("mountainThickness", FlatOverrider.createVariableFromField(EndColumn.class, "mountainThickness"))
		.addVariable("mountainMinY",      FlatOverrider.createVariableFromStaticGetterAndSetter(EndHeightOverrider.class, EndColumn.class, "getMountainMinY", "setMountainMinY"))
		.addVariable("mountainMaxY",      FlatOverrider.createVariableFromStaticGetterAndSetter(EndHeightOverrider.class, EndColumn.class, "getMountainMaxY", "setMountainMaxY"))
	);

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

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return super.setupEnvironment(environment).addAll(Y_LEVELS_ENVIRONMENT);
		}

		@Override
		public Class<EndHeightOverrider> getScriptClass() {
			return EndHeightOverrider.class;
		}
	}
}