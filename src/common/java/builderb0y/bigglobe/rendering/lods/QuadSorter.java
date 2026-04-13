package builderb0y.bigglobe.rendering.lods;

import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;

import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public abstract class QuadSorter implements SafeCloseable {

	public @Nullable Direction getGeometricNormal(VertexPacker view) {
		float
			x0  = view.x0,
			y0  = view.y0,
			z0  = view.z0,

			dx1 = view.x1 - x0,
			dy1 = view.y1 - y0,
			dz1 = view.z1 - z0,

			dx2 = view.x2 - x0,
			dy2 = view.y2 - y0,
			dz2 = view.z2 - z0,

			dx3 = view.x3 - x0,
			dy3 = view.y3 - y0,
			dz3 = view.z3 - z0,

			/*
			dx1 dy1 dz1
			dx2 dy2 dz2
			dx3 dy3 dz3
			*/
			cx = (dy2 * dz3 - dz2 * dy3),
			cy = (dz2 * dx3 - dx2 * dz3),
			cz = (dx2 * dy3 - dy2 * dx3),

			determinant = dx1 * cx + dy1 * cy + dz1 * cz;

		return this.getGeometricNormal(determinant, cx, cy, cz);
	}

	public @Nullable Direction getGeometricNormal(QuadView view) {
		float
			x0  = view.x(0),
			y0  = view.y(0),
			z0  = view.z(0),

			dx1 = view.x(1) - x0,
			dy1 = view.y(1) - y0,
			dz1 = view.z(1) - z0,

			dx2 = view.x(2) - x0,
			dy2 = view.y(2) - y0,
			dz2 = view.z(2) - z0,

			dx3 = view.x(3) - x0,
			dy3 = view.y(3) - y0,
			dz3 = view.z(3) - z0,

			/*
			dx1 dy1 dz1
			dx2 dy2 dz2
			dx3 dy3 dz3
			*/
			cx = (dy2 * dz3 - dz2 * dy3),
			cy = (dz2 * dx3 - dx2 * dz3),
			cz = (dx2 * dy3 - dy2 * dx3),

			determinant = dx1 * cx + dy1 * cy + dz1 * cz;

		return this.getGeometricNormal(determinant, cx, cy, cz);
	}

	public Direction getGeometricNormal(float determinant, float cx, float cy, float cz) {
		if (Math.abs(determinant) < 0.001F) {
			if (Math.abs(cx) < 0.001F) {
				if (Math.abs(cy) < 0.001F) {
					return cz > 0.0F ? Directions.POSITIVE_Z : Directions.NEGATIVE_Z;
				}
				else if (Math.abs(cz) < 0.001F) {
					return cy > 0.0F ? Directions.POSITIVE_Y : Directions.NEGATIVE_Y;
				}
				else {
					return null;
				}
			}
			else if (Math.abs(cy) < 0.001F) {
				if (Math.abs(cz) < 0.001F) {
					return cx > 0.0F ? Directions.POSITIVE_X : Directions.NEGATIVE_X;
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	public abstract NativeMemory getOutput(QuadView view);

	public abstract NativeMemory getOutput(VertexPacker view);

	@Environment(EnvType.CLIENT)
	public static class UnsortedQuadSorter extends QuadSorter {

		public final NativeMemory output;

		public UnsortedQuadSorter(NativeMemory output) {
			this.output = output;
		}

		@Override
		public NativeMemory getOutput(QuadView view) {
			return this.output;
		}

		@Override
		public NativeMemory getOutput(VertexPacker view) {
			return this.output;
		}

		@Override
		public void close() {
			this.output.close();
		}
	}

	@Environment(EnvType.CLIENT)
	public static class LayerQuadSorter extends QuadSorter {

		public final QuadSorter solid, cutout, translucent;

		public LayerQuadSorter(QuadSorter solid, QuadSorter cutout, QuadSorter translucent) {
			this.solid = solid;
			this.cutout = cutout;
			this.translucent = translucent;
		}

		public LayerQuadSorter(Function<ChunkSectionLayer, QuadSorter> allLayers) {
			this(allLayers.apply(ChunkSectionLayer.SOLID), allLayers.apply(ChunkSectionLayer.CUTOUT), allLayers.apply(ChunkSectionLayer.TRANSLUCENT));
		}

		public QuadSorter getNext(ChunkSectionLayer layer) {
			return switch (layer) {
				case SOLID -> this.solid;
				case CUTOUT -> this.cutout;
				case TRANSLUCENT -> this.translucent;
			};
		}

		@Override
		public NativeMemory getOutput(QuadView view) {
			return this.getNext(view.chunkLayer()).getOutput(view);
		}

		@Override
		public NativeMemory getOutput(VertexPacker view) {
			return this.getNext(view.layer).getOutput(view);
		}

		@Override
		public void close() {
			ResourceTracker.closeAll(this.solid, this.cutout, this.translucent);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FlatNormalQuadSorter extends QuadSorter {

		public final QuadSorter
			posX,
			negX,
			posZ,
			negZ,
			none;

		public FlatNormalQuadSorter(QuadSorter posX, QuadSorter negX, QuadSorter posZ, QuadSorter negZ, QuadSorter none) {
			this.posX = posX;
			this.negX = negX;
			this.posZ = posZ;
			this.negZ = negZ;
			this.none = none;
		}

		public FlatNormalQuadSorter(Function<@Nullable Direction, @NotNull QuadSorter> allFaces) {
			this(allFaces.apply(Directions.POSITIVE_X), allFaces.apply(Directions.NEGATIVE_X), allFaces.apply(Directions.POSITIVE_Z), allFaces.apply(Directions.NEGATIVE_Z), allFaces.apply(null));
		}

		public QuadSorter getNext(Direction face) {
			return switch (face) {
				case NORTH -> this.negZ;
				case EAST  -> this.posX;
				case SOUTH -> this.posZ;
				case WEST  -> this.negX;
				case null, default -> this.none;
			};
		}

		@Override
		public NativeMemory getOutput(QuadView view) {
			return this.getNext(this.getGeometricNormal(view)).getOutput(view);
		}

		@Override
		public NativeMemory getOutput(VertexPacker view) {
			return this.getNext(this.getGeometricNormal(view)).getOutput(view);
		}

		@Override
		public void close() {
			ResourceTracker.closeAll(this.posX, this.negX, this.posZ, this.negZ, this.none);
		}
	}
}