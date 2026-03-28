package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.SingleValuePalette;

/**
used for optimization in {@link SectionGenerationContext#setAllStates(BlockState, boolean)}
to fill a chunk section with a specific block state in one operation.
*/
@Mixin(SingleValuePalette.class)
public interface SingularPalette_EntryAccess {

	@Accessor("value")
	public abstract Object bigglobe_getEntry();

	@Accessor("value")
	public abstract void bigglobe_setEntry(Object entry);
}