package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class Simple2DLayer extends Layer {

	public final BlockState state;

	public Simple2DLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> top_surface,
		@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> bottom_surface,
		BlockState state
	) {
		super(valid, children, top_surface, bottom_surface);
		this.state = state;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList consumer) {
		consumer.setBlockStates(this.validMinY(column), this.validMaxY(column), this.state);
	}
}