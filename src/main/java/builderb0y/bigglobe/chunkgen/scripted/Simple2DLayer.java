package builderb0y.bigglobe.chunkgen.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

public class Simple2DLayer extends Layer {

	public final BlockState state;

	public Simple2DLayer(@VerifyNullable Valid valid, Layer[] children, BlockState state) {
		super(valid, children);
		this.state = state;
	}

	@Override
	public <B extends BlockSegmentConsumer<B>> void emitSelfSegments(ScriptedColumn column, B consumer) {
		consumer.accept(this.validMinY(column), this.validMaxY(column), this.state);
	}
}