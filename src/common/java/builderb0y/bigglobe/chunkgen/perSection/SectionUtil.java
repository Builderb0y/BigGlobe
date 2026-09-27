package builderb0y.bigglobe.chunkgen.perSection;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;

import builderb0y.bigglobe.mixins.LevelChunkSection_CountsAccess;
import builderb0y.bigglobe.mixins.PalettedContainer_DataAccess;

@SuppressWarnings("CastToIncompatibleInterface")
public class SectionUtil {

	@SuppressWarnings("unchecked")
	public static <T> PalettedContainer.Data<T> data(PalettedContainer<T> container) {
		return ((PalettedContainer_DataAccess<T>)(container)).bigglobe_getData();
	}

	public static <T> int id(PalettedContainer<T> container, T state) {
		return data(container).palette().idFor(state, container);
	}

	public static <T> BitStorage storage(PalettedContainer<T> container) {
		return data(container).storage();
	}

	public static <T> Palette<T> palette(PalettedContainer<T> container) {
		return data(container).palette();
	}

	/**
	same as {@link LevelChunkSection#recalcBlockCounts()},
	but counts palette IDs with an array instead of a hash map.
	*/
	public static void recalcBlockCounts(LevelChunkSection section) {
		PalettedContainer.Data<BlockState> data = data(section.getStates());
		Palette<BlockState> palette = data.palette();
		BitStorage storage = data.storage();
		int bits = storage.getBits();
		if (bits > 8) {
			section.recalcBlockCounts();
			return;
		}
		int paletteSize = palette.getSize();
		int[] counts = new int[paletteSize];
		if (paletteSize == 1) {
			counts[0] = storage.getSize();
		}
		else {
			//entries never span 2 longs.
			long[] raw = storage.getRaw();
			int perLong = 64 / bits;
			long mask = (1L << bits) - 1L;
			int remaining = storage.getSize();
			for (long word : raw) {
				for (int index = Math.min(perLong, remaining); --index >= 0;) {
					counts[(int)(word & mask)]++;
					word >>>= bits;
				}
				if ((remaining -= perLong) <= 0) break;
			}
		}
		int nonEmptyBlockCount = 0, tickingBlockCount = 0, fluidCount = 0, tickingFluidCount = 0;
		for (int id = 0; id < paletteSize; id++) {
			int count = counts[id];
			if (count == 0) continue;
			BlockState state = palette.valueFor(id);
			if (state.isAir()) continue;
			nonEmptyBlockCount += count;
			if (state.isRandomlyTicking()) tickingBlockCount += count;
			FluidState fluidState = state.getFluidState();
			if (!fluidState.isEmpty()) {
				fluidCount += count;
				if (fluidState.isRandomlyTicking()) tickingFluidCount += count;
			}
		}
		LevelChunkSection_CountsAccess access = (LevelChunkSection_CountsAccess)(section);
		access.bigglobe_setNonEmptyBlockCount((short)(nonEmptyBlockCount));
		access.bigglobe_setTickingBlockCount((short)(tickingBlockCount));
		access.bigglobe_setFluidCount((short)(fluidCount));
		access.bigglobe_setTickingFluidCount((short)(tickingFluidCount));
	}

	public static short checkCount(int count) {
		if (count < 0 || count > 4096) {
			throw new IllegalArgumentException("Invalid count: " + count);
		}
		return (short)(count);
	}
}