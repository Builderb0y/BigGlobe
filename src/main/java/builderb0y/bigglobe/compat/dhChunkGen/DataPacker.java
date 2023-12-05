package builderb0y.bigglobe.compat.dhChunkGen;

import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

public class DataPacker extends LongArrayList {

	public static final int
		SORT = 1 << 0,
		CHECK_INTERSECTIONS = 1 << 1,
		PAD_WITH_AIR = 1 << 2;

	public final ChunkSizedFullDataAccessor accessor;
	public final ILevelWrapper level;
	public final int bottomY, topY;

	public DataPacker(ChunkSizedFullDataAccessor accessor, ILevelWrapper level, int bottomY, int topY) {
		this.accessor = accessor;
		this.level = level;
		this.bottomY = bottomY;
		this.topY = topY;
	}

	public IBlockStateWrapper state(BlockState state) {
		return BlockStateWrapper.fromBlockState(state, this.level);
	}

	public IBiomeWrapper biome(RegistryEntry<Biome> biome) {
		return BiomeWrapper.getBiomeWrapper(biome, this.level);
	}

	public byte light(int blockLight, int skyLight) {
		return (byte)((blockLight << 4) | skyLight);
	}

	public int id(IBiomeWrapper biome, IBlockStateWrapper state) {
		return this.accessor.getMapping().addIfNotPresentAndGetId(biome, state);
	}

	public void add(int id, int minY, int maxY, byte lightLevels) {
		this.add(FullDataPointUtil.encode(id, maxY - minY, minY - this.bottomY, lightLevels));
	}

	public long[] pack(int flags) {
		if (this.isEmpty()) {
			return LongArrays.EMPTY_ARRAY;
		}
		if ((flags & SORT) != 0) {
			this.sort(
				(long l1, long l2) -> Integer.compare(
					FullDataPointUtil.getBottomY(l2),
					FullDataPointUtil.getBottomY(l1)
				)
			);
		}
		if ((flags & CHECK_INTERSECTIONS) != 0) {
			if ((flags & PAD_WITH_AIR) != 0) {
				long encoded = this.getLong(0);
				int bottom = FullDataPointUtil.getBottomY(encoded);
				int top = bottom + FullDataPointUtil.getHeight(encoded);
				int startIndex = 1;
				if (top + this.bottomY != this.topY) {
					IBiomeWrapper biome = this.accessor.getMapping().getBiomeWrapper(FullDataPointUtil.getId(encoded));
					int airID = this.accessor.getMapping().addIfNotPresentAndGetId(biome, BlockStateWrapper.AIR);
					this.add(0, FullDataPointUtil.encode(airID, this.topY - top, top, (byte)(FullDataPointUtil.getLight(encoded))));
					startIndex = 2;
				}
				for (int index = startIndex, size = this.size(); index < size; index++) {
					long newEncoded = this.getLong(index);
					int nextBottom = FullDataPointUtil.getBottomY(newEncoded);
					int nextTop = nextBottom + FullDataPointUtil.getHeight(newEncoded);
					if (nextTop != bottom) {
						IBiomeWrapper biome = this.accessor.getMapping().getBiomeWrapper(FullDataPointUtil.getId(newEncoded));
						int airID = this.accessor.getMapping().addIfNotPresentAndGetId(biome, BlockStateWrapper.AIR);
						this.add(index, FullDataPointUtil.encode(airID, bottom - nextTop, nextTop, (byte)(FullDataPointUtil.getLight(newEncoded))));
						index++;
					}
					encoded = newEncoded;
					bottom = nextBottom;
				}
				if (bottom != 0) {
					IBiomeWrapper biome = this.accessor.getMapping().getBiomeWrapper(FullDataPointUtil.getId(encoded));
					int airID = this.accessor.getMapping().addIfNotPresentAndGetId(biome, BlockStateWrapper.AIR);
					this.add(FullDataPointUtil.encode(airID, bottom, 0, (byte)(FullDataPointUtil.getLight(encoded))));
				}
			}
			else {
				long encoded = this.getLong(0);
				int bottom = FullDataPointUtil.getBottomY(encoded);
				if (bottom + FullDataPointUtil.getHeight(encoded) + this.bottomY != this.topY) {
					throw new IllegalStateException("Gap between " + FullDataPointUtil.toString(encoded) + " and <top>");
				}
				for (int index = 1, size = this.size(); index < size; index++) {
					long newEncoded = this.getLong(index);
					int nextBottom = FullDataPointUtil.getBottomY(newEncoded);
					if (nextBottom + FullDataPointUtil.getHeight(newEncoded) != bottom) {
						throw new IllegalStateException("Gap between " + FullDataPointUtil.toString(newEncoded) + " and " + FullDataPointUtil.toString(encoded));
					}
					encoded = newEncoded;
					bottom = nextBottom;
				}
				if (bottom != 0) {
					throw new IllegalStateException("Gap between <bottom> and " + FullDataPointUtil.toString(encoded));
				}
			}
		}
		return this.toLongArray();
	}
}