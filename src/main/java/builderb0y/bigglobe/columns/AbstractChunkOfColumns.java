package builderb0y.bigglobe.columns;

import java.lang.reflect.Array;
import java.util.function.Consumer;

import builderb0y.bigglobe.util.Async;

public abstract class AbstractChunkOfColumns<T_Column extends Column> {

	public final T_Column[] columns;

	@SuppressWarnings("unchecked")
	public AbstractChunkOfColumns(ColumnFactory<T_Column> factory, int length) {
		T_Column firstColumn = factory.create(0, 0);
		(this.columns = (T_Column[])(Array.newInstance(firstColumn.getClass(), length)))[0] = firstColumn;
	}

	@FunctionalInterface
	public static interface ColumnFactory<T_Column extends Column> {

		public abstract T_Column create(int x, int z);
	}

	public abstract boolean isForBiomes();

	@SuppressWarnings("unchecked")
	public <T_NewColumn extends Column> AbstractChunkOfColumns<T_NewColumn> asType(Class<T_NewColumn> columnClass) {
		if (this.columns.getClass().getComponentType() == columnClass) return (AbstractChunkOfColumns<T_NewColumn>)(this);
		else throw new IllegalStateException("Chunk of columns holds the wrong column type! Requested " + columnClass + ", got " + this.columns.getClass().getComponentType());
	}

	@SuppressWarnings("unchecked")
	public <T_NewColumn extends Column> AbstractChunkOfColumns<? extends T_NewColumn> asSubType(Class<T_NewColumn> columnClass) {
		if (columnClass.isAssignableFrom(this.columns.getClass().getComponentType())) return (AbstractChunkOfColumns<? extends T_NewColumn>)(this);
		else throw new IllegalStateException("Chunk of columns holds the wrong column type! Requested subtype of " + columnClass + ", got " + this.columns.getClass().getComponentType());
	}

	public static void checkStart(int startX, int startZ) {
		if (((startX | startZ) & 15) != 0) {
			throw new IllegalArgumentException("Start position not divisible by 16: " + startX + ", " + startZ);
		}
	}

	public abstract void setPosUnchecked(int startX, int startZ);

	public void setPos(int startX, int startZ) {
		T_Column column = this.columns[0];
		if (column.x != startX || column.z != startZ) {
			this.setPosUnchecked(startX, startZ);
		}
	}

	public abstract void setPosUncheckedAndPopulate(int startX, int startZ, Consumer<? super T_Column> populator);

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
		Async.forEach(this.columns, populator);
	}
}