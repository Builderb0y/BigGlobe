package builderb0y.bigglobe.rendering.lods;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class LightweightSection {

	public static enum SectionIndexRange {
		LOD0(0, 4096, false),
		LOD1(0,  512, true ),
		LOD2(LOD1.end, LOD1.end + 64, true),
		LOD3(LOD2.end, LOD2.end +  8, true),
		LOD4(LOD3.end, LOD3.end +  1, true);

		public static final SectionIndexRange[] VALUES = values();

		public final int start, end;
		public final boolean useLodStorage;

		SectionIndexRange(int start, int end, boolean storage) {
			this.start = start;
			this.end = end;
			this.useLodStorage = storage;
		}
	}

	public final Palette<BlockState> palette;
	public final PaletteStorage mainBlocks, lodBlocks;
	public final @Nullable LightweightSection.LightLevelStorage mainSkylight, lodSkylight;

	public LightweightSection(PalettedContainer<BlockState> container, @Nullable ChunkNibbleArray skylight) {
		this.palette = SectionUtil.palette(container);
		this.mainBlocks = SectionUtil.storage(container);
		if (this.mainBlocks instanceof EmptyPaletteStorage) {
			this.lodBlocks = new EmptyPaletteStorage(SectionIndexRange.LOD4.end);
		}
		else {
			this.lodBlocks = new PackedIntegerArray(this.mainBlocks.getElementBits(), SectionIndexRange.LOD4.end);
			CountsMap counts = new CountsMap();
			mip((int srcLod, int srcIndex, int srcShift, int dstIndex) -> {
				this.lodBlocks.set(
					dstIndex,
					this.computeMostCommonBlock(
						srcLod == 0 ? this.mainBlocks : this.lodBlocks,
						counts,
						srcIndex,
						srcShift
					)
				);
			});
		}
		this.mainSkylight = skylight != null && skylight.bytes != null ? new LightLevelStorage(skylight) : null;
		if (this.mainSkylight == null) {
			this.lodSkylight = this.mainSkylight;
		}
		else {
			this.lodSkylight = LightLevelStorage.forLength(SectionIndexRange.LOD4.end);
			mip((int srcLod, int srcIndex, int srcShift, int dstIndex) -> {
				this.lodSkylight.set(
					dstIndex,
					computeMaxLightLevel(
						srcLod == 0 ? this.mainSkylight : this.lodSkylight,
						srcIndex,
						srcShift
					)
				);
			});
		}
	}

	public static void mip(Mipper mipper) {
		for (int srcLod = 0; srcLod < 4; srcLod++) {
			int dstLod = srcLod + 1;
			int srcShift = 4 - srcLod;
			int dstShift = 4 - dstLod;
			int dstSectionSize = 1 << dstShift;
			int srcBase = SectionIndexRange.VALUES[srcLod].start;
			int dstBase = SectionIndexRange.VALUES[dstLod].start;
			for (int srcY = 0, dstY = 0; dstY < dstSectionSize; srcY += 2, dstY += 1) {
				for (int srcZ = 0, dstZ = 0; dstZ < dstSectionSize; srcZ += 2, dstZ += 1) {
					for (int srcX = 0, dstX = 0; dstX < dstSectionSize; srcX += 2, dstX += 1) {
						int srcIndex = ((srcY << (srcShift << 1)) | (srcZ << srcShift) | srcX) + srcBase;
						int dstIndex = ((dstY << (dstShift << 1)) | (dstZ << dstShift) | dstX) + dstBase;
						mipper.mip(srcLod, srcIndex, srcShift, dstIndex);
					}
				}
			}
		}
	}

	@FunctionalInterface
	public static interface Mipper {

		public void mip(int srcLod, int srcIndex, int srcShift, int dstIndex);
	}

	public int computeMostCommonBlock(PaletteStorage storage, CountsMap counts, int baseIndex, int shift) {
		int offsetX = 1;
		int offsetZ = 1 << shift;
		int offsetY = 1 << (shift << 1);
		counts.increment(storage.get(baseIndex));
		counts.increment(storage.get(baseIndex + offsetX));
		counts.increment(storage.get(baseIndex + offsetZ));
		counts.increment(storage.get(baseIndex + offsetZ + offsetX));
		counts.increment(storage.get(baseIndex + offsetY));
		counts.increment(storage.get(baseIndex + offsetY + offsetX));
		counts.increment(storage.get(baseIndex + offsetY + offsetZ));
		counts.increment(storage.get(baseIndex + offsetY + offsetZ + offsetX));
		int bestValue = counts.getMostCommonID(this.palette);
		counts.clear();
		return bestValue;
	}

	public static int computeMaxLightLevel(LightLevelStorage storage, int baseIndex, int shift) {
		int offsetX = 1;
		int offsetZ = 1 << shift;
		int offsetY = 1 << (shift << 1);
		int level = storage.get(baseIndex);
		level = Math.max(level, storage.get(baseIndex + offsetX));
		level = Math.max(level, storage.get(baseIndex + offsetZ));
		level = Math.max(level, storage.get(baseIndex + offsetZ + offsetX));
		level = Math.max(level, storage.get(baseIndex + offsetY));
		level = Math.max(level, storage.get(baseIndex + offsetY + offsetX));
		level = Math.max(level, storage.get(baseIndex + offsetY + offsetZ));
		level = Math.max(level, storage.get(baseIndex + offsetY + offsetZ + offsetX));
		return level;
	}

	public static class CountsMap {

		public final int[] counts = new int[8];

		public void increment(int id) {
			int[] counts = this.counts;
			int index = id & 7;
			while (true) {
				int value = counts[index];
				if ((value & 0xFFFF) == id) { //slot matches ID.
					value += 0x10000;
					this.counts[index] = value;
					return;
				}
				else if (value == 0) { //free spot.
					value = id | 0x10000;
					this.counts[index] = value;
					return;
				}
				else { //collision.
					index = (index + 1) & 7;
				}
			}
		}

		public void clear() {
			Arrays.fill(this.counts, 0);
		}

		public int getMostCommonID(Palette<BlockState> palette) {
			//find most common non-air block.
			int mostCommonId = 0xFFFF;
			BlockState mostCommonState = BlockStates.AIR;
			for (int newValue : this.counts) {
				if (newValue > 0xFFFF) {
					BlockState state = palette.get(newValue & 0xFFFF);
					int compare = compare(mostCommonState, state);
					if (compare > 0 || (compare == 0 && newValue > mostCommonId)) {
						mostCommonId = newValue;
						mostCommonState = state;
					}
				}
			}
			if (mostCommonId == 0xFFFF) throw new AssertionError();
			return mostCommonId & 0xFFFF;
		}

		public static int compare(BlockState oldState, BlockState newState) {
			int compare = Boolean.compare(
				BlockStateVersions.getCullingShape(newState, EmptyBlockView.INSTANCE, BlockPos.ORIGIN) == VoxelShapes.fullCube(),
				BlockStateVersions.getCullingShape(oldState, EmptyBlockView.INSTANCE, BlockPos.ORIGIN) == VoxelShapes.fullCube()
			);
			if (compare != 0) return compare;
			return Boolean.compare(isPlantLike(newState), isPlantLike(oldState));
		}

		public static boolean isPlantLike(BlockState state) {
			return !state.isAir() && state.getFluidState().getBlockState() != state;
		}
	}

	//would've just used ChunkNibbleArray directly,
	//but it's limited to length 2048 exclusively.
	//this is wasteful for LOD light levels.
	public static class LightLevelStorage {

		public final byte[] payload;

		public static LightLevelStorage forLength(int length) {
			return new LightLevelStorage(new byte[(length + 1) >> 1]);
		}

		public LightLevelStorage(byte[] payload) {
			this.payload = payload;
		}

		public LightLevelStorage(ChunkNibbleArray array) {
			this.payload = array.bytes;
		}

		public int get(int index) {
			return (this.payload[index >> 1] >> ((index & 1) << 2)) & 15;
		}

		public void set(int index, int newValue) {
			int shift = (index & 1) << 2;
			index >>= 1;
			this.payload[index] = (byte)((this.payload[index] & ~(15 << shift)) | (newValue << shift));
		}
	}
}