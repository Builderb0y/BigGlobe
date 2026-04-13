package builderb0y.bigglobe.rendering.lods.flat;

import com.mojang.serialization.DataResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.shapes.Shapes;
import builderb0y.autocodec.util.DFUVersions;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.lods.LightweightSection;
import builderb0y.bigglobe.rendering.lods.LightweightSection.LightLevelStorage;
import builderb0y.bigglobe.rendering.lods.LightweightSection.SectionIndexRange;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

@Environment(EnvType.CLIENT)
public class LightweightChunk {

	@Environment(EnvType.CLIENT)
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

	public LightweightChunk(LevelHeightAccessor world, ChunkPos pos) {
		this.minY = HeightLimitViewVersions.getMinY(world);
		this.maxY = HeightLimitViewVersions.getMaxY(world);
		this.pos = pos;
		this.columns = new BlockSegmentList[ColumnIndexRange.LOD4.end];
	}

	public void update(ServerLevel world, ListTag sectionsNBT, BlockSegmentList @Nullable [] cullingData) {
		int minChunkSectionY = this.minY >> 4;
		int maxChunkSectionY = this.maxY >> 4;
		LightweightSection[] sections = new LightweightSection[maxChunkSectionY - minChunkSectionY];
		try (AsyncRunner async = new AsyncRunner(BigGlobeThreadPool.lodExecutor())) {
			for (Tag element : sectionsNBT) {
				CompoundTag compound = (CompoundTag)(element);
				async.submit(() -> {
					if (compound.get("Y") instanceof NumericTag nbtNumber) {
						int y = nbtNumber.intValue();
						if (y >= minChunkSectionY && y < maxChunkSectionY) {
							Tag containerNBT = compound.get("block_states");
							if (containerNBT != null) {
								DataResult<PalettedContainer<BlockState>> containerResult = (
									world
									.palettedContainerFactory()
									.blockStatesContainerCodec()
									.parse(NbtOps.INSTANCE, containerNBT)
								);
								PalettedContainer<BlockState> container = DFUVersions.getResult(containerResult);
								if (container != null) {
									byte[] skylight = compound.get("SkyLight") instanceof ByteArrayTag byteArray ? byteArray.getAsByteArray() : null;
									sections[y - minChunkSectionY] = new LightweightSection(
										container,
										skylight != null ? new DataLayer(skylight) : null
									);
								}
								else {
									BigGlobeMod.LOGGER.error("Error while reading chunk data for LODs: " + DFUVersions.getMessage(containerResult));
								}
							}
						}
					}
				});
			}
		}
		this.update(sections, cullingData, world.dimensionType().hasSkyLight() ? ((byte)(15)) : ((byte)(0)));
	}

	public void update(LightweightSection[] sections, BlockSegmentList @Nullable [] cullingData, byte topLightLevel) {
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
										BitStorage storage = lod_ == 0 ? section.mainBlocks : section.lodBlocks;
										LightLevelStorage lightLevels = lod_ == 0 ? section.mainSkylight : section.lodSkylight;
										if (storage instanceof ZeroBitStorage) {
											BlockState newState = section.palette.valueFor(0);
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
													newState = section.palette.valueFor(id);
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
								this.addSegment(list, startY, endY, state, Math.max(topLightLevel - BlockStateVersions.getOpacity(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 0));
								if (cullingData != null) {
									this.cull(list, cullingData[columnIndex_]);
								}
								list.trim();
								columns[columnIndex_] = list;
							}

							public void addSegment(BlockSegmentList list, int minY, int maxY, BlockState state, int skylight) {
								if (maxY == list.minY) return; //ignore void air.
								if (BlockStateVersions.getCullingShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == Shapes.block()) {
									for (int index = list.size(); --index >= 0; ) {
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
										else if (BlockStateVersions.getCullingShape(segment.value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != Shapes.block()) {
											break;
										}
									}
								}
								assert list.isEmpty() || list.get(list.size() - 1).maxY() == minY;
								LitSegment segment = new LitSegment(minY, maxY - 1);
								segment.value = state;
								segment.skylightLevel = (byte)(skylight);
								list.add(segment);
								if (SegmentList.ASSERTS) list.checkIntegrity();
							}

							public void cull(BlockSegmentList real, BlockSegmentList cull) {
								for (int cullIndex = 0, size = cull.size(); cullIndex < size; cullIndex++) {
									LitSegment cullSegment = cull.get(cullIndex);
									if (!BlockStateVersions.isOpaqueFullCube(cullSegment.value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
										if (cullIndex == 0) return;
										int topIndex = real.getSegmentIndex(cullSegment.minY - caveCullingDepth, false);
										while (topIndex >= 0 && !BlockStateVersions.isOpaqueFullCube(real.get(topIndex).value, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
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