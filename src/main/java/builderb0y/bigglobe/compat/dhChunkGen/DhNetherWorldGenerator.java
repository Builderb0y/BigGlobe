package builderb0y.bigglobe.compat.dhChunkGen;

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;

import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeNetherChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.columns.WorldColumn;

public class DhNetherWorldGenerator extends AbstractDhWorldGenerator {

	public final BigGlobeNetherChunkGenerator generator;

	public DhNetherWorldGenerator(IDhApiLevelWrapper level, ServerWorld serverWorld, BigGlobeNetherChunkGenerator generator) {
		super(level, serverWorld);
		this.generator = generator;
	}

	@Override
	public BigGlobeChunkGenerator getGenerator() {
		return this.generator;
	}

	@Override
	public void prepareColumn(WorldColumn column) {
		NetherColumn netherColumn = (NetherColumn)(column);
		netherColumn.getLocalCell();
		netherColumn.getCaveNoise();
		netherColumn.getCavernNoise();
	}

	@Override
	public DataPointListPopulator getDataPointPopulator(int chunkX, int chunkZ) {
		return (ChunkOfColumns<? extends WorldColumn> columns, int columnIndex, DataPointListBuilder builder) -> {
			NetherColumn column = (NetherColumn)(columns.getColumn(columnIndex));
			int chunkBottomY = column.getFinalBottomHeightI();
			IDhApiBlockStateWrapper filler  = this.blockState(builder.query, column.getLocalCell().settings.filler);
			IDhApiBlockStateWrapper fluid   = this.blockState(builder.query, column.getLocalCell().settings.fluid_state);
			IDhApiBlockStateWrapper current = null;
			int lavaLevel = column.getLavaLevelI();
			int topOfSegment = column.getFinalTopHeightI();
			for (int y = topOfSegment; y >= chunkBottomY; y--) {
				IDhApiBlockStateWrapper next;
				if (column.isTerrainAt(y, true)) next = filler;
				else if (y < lavaLevel) next = fluid;
				else next = null;
				if (current != next) {
					if (current != null) {
						builder.lightLevel = current == fluid ? 0xF0 : 0x00;
						builder.add(current, y + 1, topOfSegment);
					}
					current = next;
					topOfSegment = y + 1;
				}
			}
			if (current != null) {
				builder.lightLevel = current == fluid ? 0xF0 : 0x00;
				builder.add(current, chunkBottomY, topOfSegment);
			}
		};
	}
}