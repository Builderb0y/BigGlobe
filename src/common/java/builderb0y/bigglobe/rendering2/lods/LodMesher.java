package builderb0y.bigglobe.rendering2.lods;

import java.util.ConcurrentModificationException;
import java.util.EnumMap;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;

import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.DirectionVersions;

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
					if (!centerSegment.value.isAir()) {
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
								blockRenderer.tesselateBlock(
									emitter,
									pos.getX(),
									pos.getY(),
									pos.getZ(),
									world,
									pos,
									centerSegment.value,
									this.blockModels.get(centerSegment.value),
									centerSegment.value.getSeed(pos)
								);
								FluidState fluidState = centerSegment.value.getFluidState();
								if (!fluidState.isEmpty()) {
									FluidRenderingRegistry.get(fluidState.getType()).renderFluid(
										fluidRenderer,
										pos,
										world,
										fluidOutput,
										centerSegment.value,
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

	public static boolean quickCheckRender(BlockState self, BlockState other) {
		if (BlockStateVersions.isOpaqueFullCube(other, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) return false;
		FluidState fluid = self.getFluidState();
		return fluid.createLegacyBlock() != self /* false for waterlogged blocks */ || other.getFluidState() != fluid;
	}
}