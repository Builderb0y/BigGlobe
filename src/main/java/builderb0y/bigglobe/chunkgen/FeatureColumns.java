package builderb0y.bigglobe.chunkgen;

import net.minecraft.world.StructureWorldAccess;

import builderb0y.bigglobe.columns.WorldColumn;

/**
secret stash of columns whose properties may
be overridden (see the "overriders" package).
used for some parts of feature generation.
*/
public class FeatureColumns {

	public static final ThreadLocal<ColumnSupplier> FEATURE_COLUMNS = new ThreadLocal<>();

	public static WorldColumn get(StructureWorldAccess world, int x, int z) {
		return get(world, x, z, FEATURE_COLUMNS.get());
	}

	public static WorldColumn get(StructureWorldAccess world, int x, int z, ColumnSupplier supplier) {
		if (supplier != null) {
			WorldColumn column = supplier.getColumn(x, z);
			if (column != null) {
				return column;
			}
		}
		return WorldColumn.forWorld(world, x, z);
	}

	public static interface ColumnSupplier {

		public abstract WorldColumn getColumn(int x, int z);

		public static ColumnSupplier fixedPosition(WorldColumn column) {
			return (x, z) -> column;
		}

		public static ColumnSupplier varyingPosition(WorldColumn column) {
			return (x, z) -> {
				column.setPos(x, z);
				return column;
			};
		}
	}
}