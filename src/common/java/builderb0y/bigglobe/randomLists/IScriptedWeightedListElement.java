package builderb0y.bigglobe.randomLists;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public interface IScriptedWeightedListElement extends IWeightedListElement {

	public abstract ColumnYToDoubleScript.@VerifyNullable Catcher getWeightScript();

	public default double getRestrictedWeight(ScriptedColumn column, int y) {
		double weight = this.getWeight();
		ColumnYToDoubleScript.Catcher script = this.getWeightScript();
		if (script != null) weight *= script.get(column, y);
		return weight;
	}
}