package builderb0y.bigglobe.rendering.lods.flat;

import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.rendering.lods.LodNode;
import builderb0y.bigglobe.rendering.lods.LodVertexFormat;
import builderb0y.bigglobe.rendering.lods.QuadPacker;
import builderb0y.bigglobe.rendering.lods.QuadSorter.FlatNormalQuadSorter;
import builderb0y.bigglobe.rendering.lods.QuadSorter.LayerQuadSorter;
import builderb0y.bigglobe.rendering.lods.QuadSorter.UnsortedQuadSorter;

@Environment(EnvType.CLIENT)
public class FlatQuadPacker extends QuadPacker<FlatMesh> {

	public FlatQuadPacker() {
		super(
			new LayerQuadSorter(
				(ChunkSectionLayer _) -> new FlatNormalQuadSorter(
					(@Nullable Direction direction) -> new UnsortedQuadSorter(
						new NativeMemory(
							//expect more data for vertical and un-aligned quads.
							switch (direction) {
								case NORTH, SOUTH, EAST, WEST -> LodNode.SIZE * LodVertexFormat.QUAD_BYTES * 16;
								case null, default -> LodQuadNode.AREA * LodVertexFormat.QUAD_BYTES * 4;
							}
						)
					)
				)
			)
		);
	}

	@Override
	public FlatMesh build(Supplier<String> name) {
		return new FlatMesh(this, name);
	}

	@Override
	public int encodePosition(float x, float y, float z) {
		x = x * (128.0F / LodNode.SIZE) + 64.5F;
		y = y * (32768.0F / 4096.0F) + 0.5F;
		z = z * (128.0F / LodNode.SIZE) + 64.5F;

		int ix = Mth.clamp(BigGlobeMath.floorI(x), 0, 255);
		int iy = Mth.clamp(BigGlobeMath.floorI(y), Short.MIN_VALUE, Short.MAX_VALUE);
		int iz = Mth.clamp(BigGlobeMath.floorI(z), 0, 255);

		return ix | (iz << 8) | (iy << 16);
	}
}