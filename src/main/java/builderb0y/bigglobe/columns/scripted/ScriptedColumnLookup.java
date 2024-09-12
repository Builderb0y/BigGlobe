package builderb0y.bigglobe.columns.scripted;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.util.math.ColumnPos;

import builderb0y.bigglobe.util.ScopeLocal;
import builderb0y.scripting.bytecode.MethodInfo;

public interface ScriptedColumnLookup {

	public static final ScopeLocal<ScriptedColumnLookup> GLOBAL = new ScopeLocal<>();
	public static final MethodInfo LOOKUP_COLUMN = MethodInfo.inCaller("lookupColumn");

	public abstract ScriptedColumn lookupColumn(int x, int z);

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
				this.columns = new Long2ObjectOpenHashMap<>(64);
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