package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class Simple2DLayer extends Layer {

	public final BlockState state;

	public Simple2DLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children,
		BlockState state
	) {
		super(valid, children, before_children, after_children);
		this.state = state;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList consumer) {
		consumer.setBlockStates(this.validMinY(column), this.validMaxY(column), this.state);
	}
}