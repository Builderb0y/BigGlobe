package builderb0y.bigglobe.chunkgen.perSection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.SingularPalette_EntryAccess;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class PaletteIdReplacer {

	public final short[] lookup;

	public PaletteIdReplacer(short[] lookup) {
		this.lookup = lookup;
	}

	public int getReplacement(int id) {
		return id < this.lookup.length ? this.lookup[id] : id;
	}

	public static @Nullable PaletteIdReplacer of(SectionGenerationContext context, BlockState2ObjectMap<BlockState> replacements) {
		if (replacements.runtimeStates.isEmpty()) {
			return null;
		}
		if (context.palette() instanceof SingularPalette_EntryAccess singular && singular.bigglobe_getEntry() == BlockStates.AIR) {
			return null;
		}
		PalettedContainer<BlockState> container = context.container();
		outer:
		while (true) {
			boolean changed = false;
			Palette<BlockState> palette = context.palette();
			BitStorage storage = context.storage();
			int size = palette.getSize();
			short[] lookup = new short[size];
			for (int id = 0; id < size; id++) {
				BlockState from = palette.valueFor(id);
				BlockState to = replacements.runtimeStates.get(from);
				if (to != null) {
					changed = true;
					lookup[id] = BigGlobeMath.toShortExact(palette.idFor(to, container));
				}
				else {
					lookup[id] = BigGlobeMath.toShortExact(id);
				}
				//note: it is important to check for a resize after every iteration
				//in this specific method, because of our use of a loop.
				//a resize could change the palette size, which would change our loop bound.
				//we don't want to call palette.get(id) with an invalid id,
				//because that would throw an exception.
				if (storage != (storage = context.storage())) { //resize occurred. start over.
					continue outer;
				}
			}
			return changed ? new PaletteIdReplacer(lookup) : null;
		}
	}
}