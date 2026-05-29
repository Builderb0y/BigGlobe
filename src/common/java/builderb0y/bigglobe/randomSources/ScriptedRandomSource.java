package builderb0y.bigglobe.randomSources;

import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYRNGScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;

public class ScriptedRandomSource implements RandomSource {

	public final double min;
	public final @VerifySorted(greaterThan = "min") double max;
	public final ColumnYRNGScript.Catcher script;
	public Boolean requiresColumn;

	public ScriptedRandomSource(double max, double min, ColumnYRNGScript.Catcher script) {
		this.max = max;
		this.min = min;
		this.script = script;
	}

	@Override
	public double get(ScriptedColumn column, int y, long seed) {
		return this.script.get(column, y, seed);
	}

	@Override
	public double get(ScriptedColumn column, int y, RandomGenerator random) {
		return this.get(column, y, random.nextLong());
	}

	@Override
	public boolean requiresColumn() {
		if (this.requiresColumn == null) {
			if (this.script.script == null) {
				throw new IllegalStateException("Trying to determine if script requires column before script is compiled OR script failed to compile.");
			}
			this.requiresColumn = this.script.streamDirectDependencies().map(Holder::value).anyMatch(dependency -> dependency instanceof ColumnEntry || dependency instanceof WorldTrait);
		}
		return this.requiresColumn;
	}

	@Override
	public double minValue() {
		return this.min;
	}

	@Override
	public double maxValue() {
		return this.max;
	}
}