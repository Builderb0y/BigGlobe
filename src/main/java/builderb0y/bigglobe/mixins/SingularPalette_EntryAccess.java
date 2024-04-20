package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.SingularPalette;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;

/**
used for optimization in {@link SectionGenerationContext#setAllStates(BlockState)}
to fill a chunk section with a specific block state in one operation.
*/
@Mixin(SingularPalette.class)
public interface SingularPalette_EntryAccess {

	@Accessor("entry")
	public abstract Object bigglobe_getEntry();

	@Accessor("entry")
	public abstract void bigglobe_setEntry(Object entry);
}