package builderb0y.bigglobe.columns;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder.ColumnLookup;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class ChunkOfColumns<T_Column extends Column> extends AbstractChunkOfColumns<T_Column> implements ColumnLookup {

	public ChunkOfColumns(ColumnFactory<T_Column> columnFactory) {
		super(columnFactory, 256);
		T_Column[] columns = this.columns;
		for (int index = 1; index < 256; index++) {
			columns[index] = columnFactory.create(index & 15, index >>> 4);
		}
	}

	@Override
	public boolean isForBiomes() {
		return false;
	}

	@Override
	public <T_NewColumn extends Column> ChunkOfColumns<T_NewColumn> asType(Class<T_NewColumn> columnClass) {
		return (ChunkOfColumns<T_NewColumn>)(super.asType(columnClass));
	}

	@Override
	public <T_NewColumn extends Column> ChunkOfColumns<? extends T_NewColumn> asSubType(Class<T_NewColumn> columnClass) {
		return (ChunkOfColumns<? extends T_NewColumn>)(super.asSubType(columnClass));
	}

	@Override
	public void setPosUnchecked(int startX, int startZ) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		for (int index = 0; index < 256; index++) {
			columns[index].setPosUnchecked(startX | (index & 15), startZ | (index >>> 4));
		}
	}

	@Override
	public void setPosUncheckedAndPopulate(int startX, int startZ, Consumer<? super T_Column> populator) {
		checkStart(startX, startZ);
		T_Column[] columns = this.columns;
		try (AsyncRunner async = BigGlobeThreadPool.autoRunner()) {
			for (int index = 0; index < 256; index++) {
				T_Column column = columns[index];
				column.setPosUnchecked(startX | (index & 15), startZ | (index >>> 4));
				async.submit(() -> populator.accept(column));
			}
		}
	}

	@Override
	public WorldColumn lookupColumn(int x, int z) {
		return (WorldColumn)(this.getColumnChecked(x, z));
	}

	public @Nullable T_Column getColumnChecked(int x, int z) {
		T_Column column = this.getColumn(x, z);
		return column.x == x && column.z == z ? column : null;
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
}