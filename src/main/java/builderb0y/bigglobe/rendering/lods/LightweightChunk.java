package builderb0y.bigglobe.rendering.lods;

import java.lang.ref.SoftReference;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkNibbleArray;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.lods.LightweightSection.LightLevelStorage;
import builderb0y.bigglobe.rendering.lods.LightweightSection.SectionIndexRange;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public class LightweightChunk {

	public static enum ColumnIndexRange {
		LOD0(0, 256),
		LOD1(LOD0.end, LOD0.end + 64),
		LOD2(LOD1.end, LOD1.end + 16),
		LOD3(LOD2.end, LOD2.end + 4),
		LOD4(LOD3.end, LOD3.end + 1);

		public static final ColumnIndexRange[] VALUES = values();

		public final int start, end;

		ColumnIndexRange(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	public final int minY, maxY;
	public final ChunkPos pos;
	public final BlockSegmentList[] columns;

	public LightweightChunk(HeightLimitView world, ChunkPos pos) {
		this.minY = HeightLimitViewVersions.getMinY(world);
		this.maxY = HeightLimitViewVersions.getMaxY(world);
		this.pos = pos;
		this.columns = new BlockSegmentList[ColumnIndexRange.LOD4.end];
	}

	public void update(NbtList sectionsNBT, BlockSegmentList @Nullable [] cullingData) {
		int minChunkSectionY = this.minY >> 4;
		int maxChunkSectionY = this.maxY >> 4;
		LightweightSection[] sections = new LightweightSection[maxChunkSectionY - minChunkSectionY];
		try (AsyncRunner async = new AsyncRunner(BigGlobeThreadPool.lodExecutor())) {
			for (NbtElement element : sectionsNBT) {
				NbtCompound compound = (NbtCompound)(element);
				async.submit(() -> {
					if (compound.get("Y") instanceof AbstractNbtNumber nbtNumber) {
						int y = nbtNumber.intValue();
						if (y >= minChunkSectionY && y < maxChunkSectionY) {
							NbtElement containerNBT = compound.get("block_states");
							if (containerNBT != null) {
								byte[] skylight = compound.get("SkyLight") instanceof NbtByteArray byteArray ? byteArray.getByteArray() : null;
								sections[y - minChunkSectionY] = new LightweightSection(
									#if MC_VERSION >= MC_1_21_2
										net.minecraft.world.chunk.SerializedChunk
									#else
										net.minecraft.world.ChunkSerializer
									#endif
									.CODEC.parse(NbtOps.INSTANCE, containerNBT).getOrThrow(),
									skylight != null ? new ChunkNibbleArray(skylight) : null
								);
							}
						}
					}
				});
			}
		}
		this.update(sections, cullingData);
	}

	public void update(LightweightSection[] sections, BlockSegmentList @Nullable [] cullingData) {
		int minBlockY = this.minY;
		int maxBlockY = this.maxY;
		int minSectionY = minBlockY >> 4;
		int maxSectionY = maxBlockY >> 4;
		BlockSegmentList[] columns = this.columns;
		int verticalCompression = BigGlobeConfig.INSTANCE.get().lodRendering.verticalCompression;
		int caveCullingDepth = BigGlobeConfig.INSTANCE.get().lodRendering.caveCullingDepth;
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			int columnIndex = 0;
			for (int lod = 0; lod <= 4; lod++) {
				final int lod_ = lod;
				int lodShift = 4 - lod;
				int lodSize = 1 << lodShift;
				int sectionOffset = SectionIndexRange.VALUES[lod].start;
				for (int z = 0; z < lodSize; z++) {
					final int z_ = z;
					for (int x = 0; x < lodSize; x++) {
						final int x_ = x;
						final int columnIndex_ = columnIndex;
						async.submit(new Runnable() {

							@Override
							public void run() {
								BlockSegmentList list = new BlockSegmentList(minBlockY >> lod_, ((maxBlockY - 1) >> lod_) + 1);
								//extract block states.
								int startY = list.minY;
								int endY = startY;
								int prevLightLevel = 0;
								BlockState state = BlockStates.VOID_AIR;
								for (int sectionY = minSectionY; sectionY < maxSectionY; sectionY++) {
									LightweightSection section = sections[sectionY - minSectionY];
									if (section == null) {
										if (state != BlockStates.AIR) {
											this.addSegment(list, startY, endY, state, prevLightLevel);
											state = BlockStates.AIR;
											startY = endY;
										}
										endY += lodSize;
									}
									else {
										PaletteStorage storage = lod_ == 0 ? section.mainBlocks : section.lodBlocks;
										LightLevelStorage lightLevels = lod_ == 0 ? section.mainSkylight : section.lodSkylight;
										if (storage instanceof EmptyPaletteStorage) {
											BlockState newState = section.palette.get(0);
											if (state != newState) {
												this.addSegment(list, startY, endY, state, prevLightLevel);
												state = newState;
												startY = endY;
											}
											if (lightLevels != null) {
												int index = (((lodSize - 1) << (lodShift << 1)) | (z_ << lodShift) | x_) + sectionOffset;
												prevLightLevel = lightLevels.get(index);
											}
											endY += lodSize;
										}
										else {
											int id = -1;
											BlockState newState = null;
											for (int localY = 0; localY < lodSize; localY++) {
												int index = ((localY << (lodShift << 1)) | (z_ << lodShift) | x_) + sectionOffset;
												int newID = storage.get(index);
												if (newID != id) {
													id = newID;
													newState = section.palette.get(id);
												}
												if (newState != state) {
													this.addSegment(list, startY, endY, state, prevLightLevel);
													startY = endY;
													state = newState;
												}
												if (lightLevels != null) {
													prevLightLevel = lightLevels.get(index);
												}
												endY++;
											}
										}
									}
								}
								this.addSegment(list, startY, endY, state, prevLightLevel);
								if (cullingData != null) {
									this.cull(list, cullingData[columnIndex_]);
								}
								list.trim();
								columns[columnIndex_] = list;
							}

							public void addSegment(BlockSegmentList list, int minY, int maxY, BlockState state, int skylight) {
								if (maxY == list.minY) return; //ignore void air.
								if (BlockStateVersions.getCullingShape(state, EmptyBlockView.INSTANCE, BlockPos.ORIGIN) == VoxelShapes.fullCube()) {
									for (int index = list.size(); --index >= 0;) {
										LitSegment segment = list.get(index);
										if (segment.value == state) {
											list.size(index + 1);
											segment.maxY = maxY - 1;
											segment.skylightLevel = (byte)(skylight);
											if (SegmentList.ASSERTS) list.checkIntegrity();
											return;
										}
										else if (segment.minY < minY - verticalCompression) {
											break;
										}
										else if (BlockStateVersions.getCullingShape(segment.value, EmptyBlockView.INSTANCE, BlockPos.ORIGIN) != VoxelShapes.fullCube()) {
											break;
										}
									}
								}
								assert list.isEmpty() || list.getLast().maxY() == minY;
								LitSegment segment = new LitSegment(minY, maxY - 1);
								segment.value = state;
								segment.skylightLevel = (byte)(skylight);
								list.add(segment);
								if (SegmentList.ASSERTS) list.checkIntegrity();
							}

							public void cull(BlockSegmentList real, BlockSegmentList cull) {
								for (int cullIndex = 0, size = cull.size(); cullIndex < size; cullIndex++) {
									LitSegment cullSegment = cull.get(cullIndex);
									if (!BlockStateVersions.isOpaqueFullCube(cullSegment.value, EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
										if (cullIndex == 0) return;
										int topIndex = real.getSegmentIndex(cullSegment.minY - caveCullingDepth, false);
										while (topIndex >= 0 && !BlockStateVersions.isOpaqueFullCube(real.get(topIndex).value, EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
											topIndex--;
										}
										topIndex--; //preserve surface.
										if (topIndex >= 0) {
											LitSegment top = real.get(topIndex);
											LitSegment replacement = cull.get(cullIndex - 1);
											LitSegment added = real.addSegment(real.minY, top.maxY, replacement.value);
											if (added != null) added.skylightLevel = replacement.skylightLevel;
											return;
										}
									}
								}
							}
						});
						columnIndex++;
					}
				}
			}
		}
	}

	public BlockSegmentList getColumn(int x, int z, int level) {
		int shift = 4 - level;
		int mask = (1 << shift) - 1;
		int packed = ((z & mask) << shift) | (x & mask);
		ColumnIndexRange range = ColumnIndexRange.VALUES[level];
		assert packed >= 0 && packed < range.end - range.start;
		return this.columns[packed + range.start];
	}

	public BlockState getBlockState(int x, int y, int z) {
		BlockSegmentList column = this.columns[((z & 15) << 4) | (x & 15)];
		if (column == null) return BlockStates.VOID_AIR;
		BlockState state = column.getBlockState(y);
		if (state == null) return BlockStates.VOID_AIR;
		return state;
	}
}