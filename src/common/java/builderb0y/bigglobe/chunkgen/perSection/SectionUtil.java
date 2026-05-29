package builderb0y.bigglobe.chunkgen.perSection;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

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

	public static short checkCount(int count) {
		if (count < 0 || count > 4096) {
			throw new IllegalArgumentException("Invalid count: " + count);
		}
		return (short)(count);
	}
}