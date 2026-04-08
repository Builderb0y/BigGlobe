package builderb0y.bigglobe.rendering2.lods;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import builderb0y.bigglobe.ClientState.ColorScript;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator.GameMechanics.ColorOverrides;
import builderb0y.bigglobe.chunkgen.ScriptedColumnBiomeSource;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.SafeCloseable;

public class ColumnBlockGetter implements BlockAndTintGetter, SafeCloseable {

	public final BlockSegmentList[] storage;
	public final ScriptedColumn[] columns;
	public final BoundingBox unpaddedVolume, paddedVolume;
	public final byte lod, ambientLight;
	public final ColorOverrides colorOverrides;
	public final ScriptedColumnBiomeSource biomeSource;
	public final CardinalLighting lighting;
	public final Consumer<ScriptedColumn[]> cleanup;

	public ColumnBlockGetter(
		BlockSegmentList[]         storage,
		ScriptedColumn[]           columns,
		BoundingBox                unpaddedVolume,
		BoundingBox                paddedVolume,
		byte                       lod,
		byte                       ambientLight,
		ColorOverrides             colorOverrides,
		ScriptedColumnBiomeSource  biomeSource,
		CardinalLighting           lighting,
		Consumer<ScriptedColumn[]> cleanup
	) {
		this.storage        = storage;
		this.columns        = columns;
		this.unpaddedVolume = unpaddedVolume;
		this.paddedVolume   = paddedVolume;
		this.lod            = lod;
		this.ambientLight   = ambientLight;
		this.colorOverrides = colorOverrides;
		this.biomeSource    = biomeSource;
		this.lighting       = lighting;
		this.cleanup        = cleanup;
	}

	@Override
	public void close() {
		if (this.cleanup != null) this.cleanup.accept(this.columns);
	}

	@Override
	public CardinalLighting cardinalLighting() {
		return this.lighting;
	}

	public int relativeColumnIndex(int x, int z) {
		return z * this.paddedVolume.getXSpan() + x;
	}

	public int columnIndex(int x, int z) {
		if (x >= this.paddedVolume.minX() && x <= this.paddedVolume.maxX() && z >= this.paddedVolume.minZ() && z <= this.paddedVolume.maxZ()) {
			x -= this.paddedVolume.minX();
			z -= this.paddedVolume.minZ();
			return z * this.paddedVolume.getXSpan() + x;
		}
		return -1;
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver resolver) {
		if (this.paddedVolume.isInside(pos)) {
			int relativeX = pos.getX() - this.paddedVolume.minX();
			int relativeZ = pos.getZ() - this.paddedVolume.minZ();
			int y = pos.getY() << this.lod;
			int index = relativeZ * this.paddedVolume.getXSpan() + relativeX;
			ScriptedColumn column = this.columns[index];
			ColorScript.Catcher script = this.colorOverrides.forColorResolver(resolver);
			if (script != null) {
				return script.getColor(column, y);
			}
			else if (this.biomeSource != null) {
				return resolver.getColor(this.biomeSource.script.get(column, y).entry.value(), column.x(), column.z());
			}
		}
		return -1;
	}

	@Override
	public int getBrightness(LightLayer layer, BlockPos pos) {
		LitSegment segment = this.getSegment(pos);
		if (segment != null) {
			return switch (layer) {
				case BLOCK -> segment.getBlockLight();
				case SKY -> segment.getSkyLight(pos.getY(), this.lod);
			};
		}
		else {
			return switch (layer) {
				case BLOCK -> 0;
				case SKY -> this.ambientLight;
			};
		}
	}

	@Override
	public int getRawBrightness(BlockPos pos, int darkening) {
		LitSegment segment = this.getSegment(pos);
		if (segment != null) {
			int block = segment.getBlockLight();
			int sky = segment.getSkyLight(pos.getY(), this.lod) - darkening;
			return Math.max(block, sky);
		}
		else {
			return Math.max(this.ambientLight - darkening, 0);
		}
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return LevelLightEngine.EMPTY;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return null;
	}

	public @Nullable LitSegment getSegment(BlockPos pos) {
		BlockSegmentList list = this.getList(pos.getX(), pos.getZ());
		return list != null ? list.getOverlappingSegment(pos.getY()) : null;
	}

	public @Nullable BlockSegmentList getList(int x, int z) {
		int index = this.columnIndex(x, z);
		return index >= 0 ? this.storage[index] : null;
	}

	public @Nullable ScriptedColumn getColumn(int x, int z) {
		int index = this.columnIndex(x, z);
		return index >= 0 ? this.columns[index] : null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		LitSegment segment = this.getSegment(pos);
		return segment != null ? segment.value : BlockStates.AIR;
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return this.getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return this.paddedVolume.getYSpan();
	}

	@Override
	public int getMinY() {
		return this.paddedVolume.minY();
	}

	@Override
	public int getMaxY() {
		return this.paddedVolume.maxY();
	}
}