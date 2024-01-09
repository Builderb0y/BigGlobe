package builderb0y.bigglobe.columns.scripted;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;

public class ScriptedColumn {

	public final BigGlobeChunkGenerator generator; //todo: replace with BigGlobeScriptedChunkGenerator once that's added.
	public int x, z;
	/** the upper and lower bounds of the area that can be cached. */
	public int minY, maxY;
	public final ColumnEntry[] values;

	public ScriptedColumn(BigGlobeChunkGenerator generator, ColumnEntryRegistry registry, int x, int z, int minY, int maxY) {
		this.generator = generator;
		this.values = registry.createEntries();
		this.x = x;
		this.z = z;
		this.minY = minY;
		this.maxY = maxY;
	}

	public void clear() {
		for (ColumnEntry entry : this.values) {
			if (entry != null) entry.clear();
		}
	}

	public void setPosUnchecked(int x, int z, int minY, int maxY) {
		this.x = x;
		this.z = z;
		this.minY = minY;
		this.maxY = maxY;
		this.clear();
	}

	public void setPos(int x, int z, int minY, int maxY) {
		if (this.x != x || this.z != z || this.minY != minY || this.maxY != maxY) {
			this.setPosUnchecked(x, z, minY, maxY);
		}
	}
}