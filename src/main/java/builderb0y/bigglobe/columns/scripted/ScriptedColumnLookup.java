package builderb0y.bigglobe.columns.scripted;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.util.math.ColumnPos;

public interface ScriptedColumnLookup {

	public abstract ScriptedColumn lookupColumn(int x, int z);

	public static ScriptedColumnLookup synchronize(ScriptedColumnLookup lookup) {
		return (int x, int z) -> {
			synchronized (lookup) {
				return lookup.lookupColumn(x, z);
			}
		};
	}

	public static class Impl implements ScriptedColumnLookup, Long2ObjectFunction<ScriptedColumn> {

		public final ScriptedColumn.Factory columnFactory;
		public final ScriptedColumn.Params params;
		public Long2ObjectOpenHashMap<ScriptedColumn> columns;

		public Impl(ScriptedColumn.Factory factory, ScriptedColumn.Params params) {
			this.columnFactory = factory;
			this.params = params;
		}

		@Override
		public ScriptedColumn lookupColumn(int x, int z) {
			if (this.columns == null) {
				this.columns = new Long2ObjectOpenHashMap<>(16);
			}
			return this.columns.computeIfAbsent(ColumnPos.pack(x, z), this);
		}

		@Override
		public ScriptedColumn get(long packedPos) {
			return this.columnFactory.create(
				this.params.at(
					ColumnPos.getX(packedPos),
					ColumnPos.getZ(packedPos)
				)
			);
		}
	}
}