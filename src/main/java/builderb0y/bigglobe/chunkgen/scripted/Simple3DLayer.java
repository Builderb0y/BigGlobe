package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class Simple3DLayer extends Layer {

	public final BlockState state;
	public final ScriptUsage<GenericScriptTemplateUsage> condition;

	public Simple3DLayer(@VerifyNullable Valid valid, Layer[] children, BlockState state, ScriptUsage<GenericScriptTemplateUsage> condition) {
		super(valid, children);
		this.state = state;
		this.condition = condition;
	}

	@Override
	public void emitSelfLayers(ScriptedColumn column, BlockSegmentConsumer consumer) {

	}
}