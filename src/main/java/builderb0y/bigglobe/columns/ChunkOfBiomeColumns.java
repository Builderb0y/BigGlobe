package builderb0y.bigglobe.columns;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class ChunkOfBiomeColumns<T_Column extends WorldColumn> {

	public final T_Column[] columns;

	public ChunkOfBiomeColumns(IntFunction<T_Column[]> arrayFactory, ColumnFactory<T_Column> columnFactory) {
		T_Column[] columns = this.columns = arrayFactory.apply(16);
		for (int index = 0; index < 16; index++) {
			columns[index] = columnFactory.create((index & 3) << 2, index & 0b1100);
		}
	}

	@FunctionalInterface
	public static interface ColumnFactory<T_Column extends WorldColumn> {

		public abstract T_Column create(int x, int z);
	}

	public T_Column getColumn(int index) {
		return this.columns[index];
	}

	public T_Column getColumn(int x, int z) {
		return this.columns[(z & 0b1100) | ((x & 3) << 2)];
	}

	public static void checkStart(int startX, int startZ) {
		if (((startX | startZ) & 15) != 0) {
			throw new IllegalArgumentException("Start position not divisible by 16: " + startX + ", " + startZ);
		}
	}

	public void setPosUnchecked(int startX, int startZ) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		for (int index = 0; index < 16; index++) {
			columns[index].setPosUnchecked(startX | ((index & 3) << 2), startZ | (index & 0b1100));
		}
	}

	public void setPos(int startX, int startZ) {
		T_Column column = this.columns[0];
		if (column.x != startX || column.z != startZ) {
			this.setPosUnchecked(startX, startZ);
		}
	}

	public void setPosUncheckedAndPopulate(int startX, int startZ, Consumer<? super T_Column> populator) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		IntStream
		.range(0, 16)
		.parallel()
		.forEach((int index) -> {
			T_Column column = columns[index];
			column.setPosUnchecked(startX | ((index & 3) << 2), startZ | (index & 0b1100));
			populator.accept(column);
		});
	}

	public void setPosAndPopulate(int startX, int startZ, Consumer<? super T_Column> populator) {
		T_Column column = this.columns[0];
		if (column.x != startX || column.z != startZ) {
			this.setPosUncheckedAndPopulate(startX, startZ, populator);
		}
		else {
			this.populate(populator);
		}
	}

	public void populate(Consumer<? super T_Column> populator) {
		Arrays.stream(this.columns).parallel().forEach(populator);
	}
}