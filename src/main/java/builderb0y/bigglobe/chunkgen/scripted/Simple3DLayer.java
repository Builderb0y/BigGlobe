package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBooleanScript;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public class Simple3DLayer extends Layer {

	public final BlockState state;
	public final ScriptUsage<GenericScriptTemplateUsage> condition;
	public ColumnYToBooleanScript.Holder compiledCondition;

	public Simple3DLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> top_surface,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> bottom_surface,
		BlockState state,
		ScriptUsage<GenericScriptTemplateUsage> condition
	) {
		super(valid, children, top_surface, bottom_surface);
		this.state = state;
		this.condition = condition;
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
		this.compiledCondition = new ColumnYToBooleanScript.Holder(this.condition, registry);
		super.compile(registry);
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList consumer) {
		int minY = Math.max(this.validMinY(column), consumer.minY());
		int maxY = Math.min(this.validMaxY(column), consumer.maxY());
		int start = minY;
		boolean haveState = this.compiledCondition.get(column, minY);
		for (int y = minY; ++y < maxY;) {
			boolean nextState = this.compiledCondition.get(column, y);
			if (haveState != nextState) {
				if (haveState) consumer.setBlockStates(start, y, this.state);
				haveState = nextState;
				start = y;
			}
		}
		if (haveState) consumer.setBlockStates(start, maxY, this.state);
	}
}