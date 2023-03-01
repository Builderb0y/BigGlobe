package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.SingularPalette;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.overriders.CachedStructures;

/**
used for optimization in {@link BigGlobeOverworldChunkGenerator#generateRawSectionsAndCaves(Chunk, ChunkOfColumns, CachedStructures, boolean)}.
we first make a guess as to what the majority of blocks in the chunk section will be,
based on information at the center of the chunk section,
and set the entire chunk section to that block with this hook.
we clean up any inaccuracies in that guess afterwards.
in many cases, this represents 4096 block setting
operations that we *don't* need to perform.
*/
@Mixin(SingularPalette.class)
public interface SingularPalette_EntryAccess {

	@Accessor("entry")
	public abstract Object bigglobe_getEntry();

	@Accessor("entry")
	public abstract void bigglobe_setEntry(Object entry);
}