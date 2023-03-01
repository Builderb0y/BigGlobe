package builderb0y.bigglobe.columns;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public class ChunkOfColumns<T_Column extends Column> {

	public final T_Column[] columns;

	public ChunkOfColumns(IntFunction<T_Column[]> arrayFactory, ColumnFactory<T_Column> columnFactory) {
		T_Column[] columns = this.columns = arrayFactory.apply(256);
		for (int index = 0; index < 256; index++) {
			columns[index] = columnFactory.create(index & 15, index >>> 4);
		}
	}

	@FunctionalInterface
	public static interface ColumnFactory<T_Column extends Column> {

		public abstract T_Column create(int x, int z);
	}

	public static void checkStart(int startX, int startZ) {
		if (((startX | startZ) & 15) != 0) {
			throw new IllegalArgumentException("Start position not divisible by 16: " + startX + ", " + startZ);
		}
	}

	public void setPosUnchecked(int startX, int startZ) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		for (int index = 0; index < 256; index++) {
			columns[index].setPosUnchecked(startX | (index & 15), startZ | (index >>> 4));
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
		.range(0, 256)
		.parallel()
		.forEach((int index) -> {
			T_Column column = columns[index];
			column.setPosUnchecked(startX | (index & 15), startZ | (index >>> 4));
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

	public @Nullable T_Column getColumnChecked(int x, int z) {
		T_Column first = this.columns[0];
		if (x >= first.x && z >= first.z && x <= (first.x | 15) && z <= (first.z | 15)) {
			return this.getColumn(x, z);
		}
		else {
			return null;
		}
	}

	public T_Column getColumn(int index) {
		return this.columns[index];
	}

	public T_Column getColumn(int x, int z) {
		return this.columns[toIndex(x, z)];
	}

	public static int toIndex(int x, int z) {
		return ((z & 15) << 4) | (x & 15);
	}

	public Stream<T_Column> stream() {
		return Arrays.stream(this.columns);
	}

	public Stream<T_Column> parallelStream() {
		return this.stream().parallel();
	}
}