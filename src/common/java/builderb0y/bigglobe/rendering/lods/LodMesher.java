package builderb0y.bigglobe.rendering.lods;

import java.util.ConcurrentModificationException;
import java.util.EnumMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;

import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.BlockStateProvider;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.FixedBlockStateProvider;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.StandardBlockStatePredicate;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.DirectionVersions;

@Environment(EnvType.CLIENT)
public class LodMesher {

	public static final ScopedValue<Boolean> MESHING_LODS = ScopedValue.newInstance();

	public final boolean ambientOcclusion;
	public final BlockColors blockColors;
	public final BlockStateModelSet blockModels;
	public final FluidStateModelSet fluidModels;

	public LodMesher(boolean ambientOcclusion) {
		this.ambientOcclusion = ambientOcclusion;
		this.blockColors = Minecraft.getInstance().getBlockColors();
		this.blockModels = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		this.fluidModels = Minecraft.getInstance().getModelManager().getFluidStateModelSet();
	}

	public LodMesher() {
		this(Minecraft.getInstance().options.ambientOcclusion().get());
	}

	public static boolean isMeshing() {
		return MESHING_LODS.orElse(Boolean.FALSE);
	}

	public void mesh(ColumnBlockGetter world, QuadPacker<?> output) {
		ScopedValue.where(MESHING_LODS, Boolean.TRUE).run(() -> this.doMesh(world, output));
	}

	public void doMesh(ColumnBlockGetter world, QuadPacker<?> output) {
		QuadEmitter emitter = Renderer.get().quadEmitter(output);
		EnumMap<ChunkSectionLayer, VertexPacker> fluidOutputs = new EnumMap<>(ChunkSectionLayer.class);
		FluidRenderer.Output fluidOutput = (ChunkSectionLayer layer) -> {
			return fluidOutputs.computeIfAbsent(layer, (ChunkSectionLayer theSameLayer) -> {
				return new VertexPacker(theSameLayer, output);
			});
		};
		AltModelBlockRenderer blockRenderer = Renderer.get().altModelBlockRenderer(this.ambientOcclusion, true, this.blockColors);
		FluidRenderer fluidRenderer = new FluidRenderer(this.fluidModels);

		BlockSegmentList[] adjacents = new BlockSegmentList[4];
		MutableBlockPos pos = new MutableBlockPos();
		BoundingBox area = world.unpaddedVolume;
		for (pos.setZ(area.minZ()); pos.getZ() <= area.maxZ(); pos.setZ(pos.getZ() + 1)) {
			for (pos.setX(area.minX()); pos.getX() <= area.maxX(); pos.setX(pos.getX() + 1)) {
				ScriptedColumn column = world.getColumn(pos);
				BlockSegmentList center = world.getList(pos.getX(), pos.getZ());
				adjacents[DirectionVersions.horizontal(Directions.POSITIVE_X)] = world.getList(pos.getX() + 1, pos.getZ());
				adjacents[DirectionVersions.horizontal(Directions.NEGATIVE_X)] = world.getList(pos.getX() - 1, pos.getZ());
				adjacents[DirectionVersions.horizontal(Directions.POSITIVE_Z)] = world.getList(pos.getX(), pos.getZ() + 1);
				adjacents[DirectionVersions.horizontal(Directions.NEGATIVE_Z)] = world.getList(pos.getX(), pos.getZ() - 1);
				segmentIndexLoop:
				for (int centerIndex = center.getSegmentIndex(area.minY(), false), centerSize = center.size(); centerIndex < centerSize; centerIndex++) {
					if (center.size() != centerSize) {
						throw new ConcurrentModificationException();
					}
					LitSegment centerSegment = center.get(centerIndex);
					if (centerSegment.value.query(StandardBlockStatePredicate.AIR) <= 0) {
						for (pos.setY(Math.max(centerSegment.minY, area.minY())); pos.getY() <= centerSegment.maxY;) {
							int y = pos.getY();
							if (y > area.maxY()) break segmentIndexLoop;
							int nextY;
							boolean shouldRender;
							if (
								(
									y == centerSegment.minY &&
									centerIndex - 1 >= 0 &&
									quickCheckRender(centerSegment.value, center.get(centerIndex - 1).value)
								)
								||
								(
									y == centerSegment.maxY &&
									centerIndex + 1 < centerSize &&
									quickCheckRender(centerSegment.value, center.get(centerIndex + 1).value)
								)
							) {
								shouldRender = true;
								nextY = y + 1;
							}
							else {
								shouldRender = false;
								int skipTo = centerSegment.maxY;
								for (Direction direction : Directions.HORIZONTAL) {
									BlockSegmentList adjacent = adjacents[DirectionVersions.horizontal(direction)];
									LitSegment adjacentSegment = adjacent.getOverlappingSegment(y);
									if (adjacentSegment == null || quickCheckRender(centerSegment.value, adjacentSegment.value)) {
										shouldRender = true;
										skipTo = y + 1;
										break;
									}
									else {
										skipTo = Math.min(skipTo, adjacentSegment.maxY + 1);
									}
								}
								nextY = Math.max(skipTo, y + 1);
							}
							if (shouldRender) {
								BlockState centerState = centerSegment.getBlockState(column, pos.getY() << world.lod);
								blockRenderer.tesselateBlock(
									emitter,
									pos.getX(),
									pos.getY(),
									pos.getZ(),
									world,
									pos,
									centerState,
									this.blockModels.get(centerState),
									centerState.getSeed(pos)
								);
								FluidState fluidState = centerState.getFluidState();
								if (!fluidState.isEmpty()) {
									FluidRenderingRegistry.get(fluidState.getType()).renderFluid(
										fluidRenderer,
										pos,
										world,
										fluidOutput,
										centerState,
										fluidState
									);
								}
							}
							pos.setY(nextY);
						}
					}
				}
			}
		}
	}

	public static boolean quickCheckRender(BlockStateProvider self, BlockStateProvider other) {
		if (other.query(StandardBlockStatePredicate.SOLID) > 0) {
			return false;
		}
		if (self.query(StandardBlockStatePredicate.IS_FLUID) > 0 && other.query(StandardBlockStatePredicate.IS_FLUID) > 0) {
			return false;
		}
		return true;
	}
}