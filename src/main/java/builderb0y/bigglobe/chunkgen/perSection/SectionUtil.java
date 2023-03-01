package builderb0y.bigglobe.chunkgen.perSection;

import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;

import builderb0y.bigglobe.mixins.ChunkSection_CountsAccess;
import builderb0y.bigglobe.mixins.PalettedContainer_DataAccess;

@SuppressWarnings("CastToIncompatibleInterface")
public class SectionUtil {

	@SuppressWarnings("unchecked")
	public static <T> PalettedContainer.Data<T> data(PalettedContainer<T> container) {
		return ((PalettedContainer_DataAccess<T>)(container)).bigglobe_getData();
	}

	public static <T> int id(PalettedContainer<T> container, T state) {
		return data(container).palette().index(state);
	}

	public static <T> PaletteStorage storage(PalettedContainer<T> container) {
		return data(container).storage();
	}

	public static <T> Palette<T> palette(PalettedContainer<T> container) {
		return data(container).palette();
	}

	public static short checkCount(int count) {
		if (count < 0 || count > 4096) {
			throw new IllegalArgumentException("Invalid count: " + count);
		}
		return (short)(count);
	}

	public static int getNonEmptyBlocks(ChunkSection section) {
		return ((ChunkSection_CountsAccess)(section)).bigglobe_getNonEmptyBlockCount();
	}

	public static int getNonEmptyFluids(ChunkSection section) {
		return ((ChunkSection_CountsAccess)(section)).bigglobe_getRandomTickableFluidCount();
	}

	public static int getRandomTicking(ChunkSection section) {
		return ((ChunkSection_CountsAccess)(section)).bigglobe_getRandomTickableBlockCount();
	}

	public static void setNonEmptyBlocks(ChunkSection section, int count) {
		((ChunkSection_CountsAccess)(section)).bigglobe_setNonEmptyBlockCount(checkCount(count));
	}

	public static void setRandomTickingBlocks(ChunkSection section, int count) {
		((ChunkSection_CountsAccess)(section)).bigglobe_setRandomTickableBlockCount(checkCount(count));
	}

	public static void setRandomTickingFluids(ChunkSection section, int count) {
		((ChunkSection_CountsAccess)(section)).bigglobe_setRandomTickableFluidCount(checkCount(count));
	}
}