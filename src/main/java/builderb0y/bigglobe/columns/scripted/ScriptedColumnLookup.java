package builderb0y.bigglobe.columns.scripted;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.util.math.ColumnPos;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

public interface ScriptedColumnLookup {

	public abstract ScriptedColumn lookupColumn(int x, int z);

	public static class Impl implements ScriptedColumnLookup, Long2ObjectFunction<ScriptedColumn> {

		public final BigGlobeScriptedChunkGenerator generator;
		public final boolean distantHorizons;
		public Long2ObjectOpenHashMap<ScriptedColumn> columns;

		public Impl(BigGlobeScriptedChunkGenerator generator, boolean distantHorizons) {
			this.generator = generator;
			this.distantHorizons = distantHorizons;
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
			return this.generator.columnEntryRegistry.columnFactory.create(
				this.generator.seed,
				ColumnPos.getX(packedPos),
				ColumnPos.getZ(packedPos),
				this.generator.height.min_y(),
				this.generator.height.max_y(),
				this.distantHorizons
			);
		}
	}
}