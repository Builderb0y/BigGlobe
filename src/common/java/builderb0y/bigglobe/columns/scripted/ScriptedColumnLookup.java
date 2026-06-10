package builderb0y.bigglobe.columns.scripted;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.server.level.ColumnPos;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.util.BetterScopedValue;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

public interface ScriptedColumnLookup {

	public static final BetterScopedValue<ScriptedColumnLookup>
		GLOBAL = new BetterScopedValue<>();
	public static final TypeInfo
		TYPE = TypeInfo.of(ScriptedColumnLookup.class);
	public static final MethodInfo
		LOOKUP_COLUMN = MethodInfo.inCaller("lookupColumn"),
		HINTS = MethodInfo.inCaller("getHints");

	public abstract ConfiguredColumnFactory getSource();

	public default Hints getHints() {
		return this.getSource().hints();
	}

	public abstract ScriptedColumn lookupColumn(int x, int z);

	public static class Impl implements ScriptedColumnLookup, Long2ObjectFunction<ScriptedColumn> {

		public final ConfiguredColumnFactory factory;
		public Long2ObjectOpenHashMap<ScriptedColumn> columns;

		@Deprecated
		public Impl(ScriptedColumn.Factory factory, ScriptedColumn.Params params) {
			this.factory = new ConfiguredColumnFactory(factory, params.worldInfo(), params.hints());
		}

		public Impl(ConfiguredColumnFactory factory) {
			this.factory = factory;
		}

		@Override
		public ConfiguredColumnFactory getSource() {
			return this.factory;
		}

		@Override
		public ScriptedColumn lookupColumn(int x, int z) {
			if (this.columns == null) {
				this.columns = new Long2ObjectOpenHashMap<>(64);
			}
			return this.columns.computeIfAbsent(ColumnPos.asLong(x, z), this);
		}

		@Override
		public ScriptedColumn get(long packedPos) {
			return this.factory.createAt(
				ColumnPos.getX(packedPos),
				ColumnPos.getZ(packedPos)
			);
		}
	}
}