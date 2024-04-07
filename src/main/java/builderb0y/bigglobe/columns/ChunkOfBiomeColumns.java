package builderb0y.bigglobe.columns;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class ChunkOfBiomeColumns<T_Column extends WorldColumn> extends AbstractChunkOfColumns<T_Column> {

	public ChunkOfBiomeColumns(ColumnFactory<T_Column> columnFactory) {
		super(columnFactory, 16);
		T_Column[] columns = this.columns;
		for (int index = 1; index < 16; index++) {
			columns[index] = columnFactory.create((index & 3) << 2, index & 0b1100);
		}
	}

	@Override
	public boolean isForBiomes() {
		return true;
	}

	public T_Column getColumn(int index) {
		return this.columns[index];
	}

	public T_Column getColumn(int x, int z) {
		return this.columns[(z & 0b1100) | ((x & 3) << 2)];
	}

	@Override
	public void setPosUnchecked(int startX, int startZ) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		for (int index = 0; index < 16; index++) {
			columns[index].setPosUnchecked(startX | ((index & 3) << 2), startZ | (index & 0b1100));
		}
	}

	@Override
	public void setPosUncheckedAndPopulate(int startX, int startZ, Consumer<? super T_Column> populator) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		try (AsyncRunner async = BigGlobeThreadPool.autoRunner()) {
			for (int index = 0; index < 16; index++) {
				T_Column column = columns[index];
				column.setPosUnchecked(startX | ((index & 3) << 2), startZ | (index & 0b1100));
				async.submit(() -> populator.accept(column));
			}
		}
	}
}