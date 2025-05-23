package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import builderb0y.bigglobe.lods.VertexHeap.Slice;

@Environment(EnvType.CLIENT)
public record LodPasses(
	@Nullable Geometry opaque,
	@Nullable Geometry translucent
)
implements SafeCloseable {

	public @Nullable Geometry getGeometry(boolean translucent) {
		return translucent ? this.translucent : this.opaque;
	}

	@Override
	public void close() {
		ResourceTracker.closeAll(this.opaque, this.translucent);
	}

	@Environment(EnvType.CLIENT)
	public static record Geometry(
		CompactVertexFormat format,
		VertexHeap.Slice slice,
		int vertexCount,
		int indexCount
	)
	implements SafeCloseable {

		public static Geometry from(CompactVertexConsumer builder, VertexHeap heap) {
			try (builder) {
				if ((builder.vertexCount & 3) != 0) {
					throw new IllegalArgumentException("Incomplete quad!");
				}
				Slice slice = heap.allocate(builder.memory.address, builder.memory.used);
				if (slice != null) {
					return new Geometry(
						builder.format,
						slice,
						builder.vertexCount,
						builder.vertexCount / 4 * 6
					);
				}
				else {
					return null;
				}
			}
		}

		public int baseVertex() {
			return Math.toIntExact(this.slice.key.position() / this.format.byteStride);
		}

		@Override
		public void close() {
			this.slice.close();
		}
	}

	@Environment(EnvType.CLIENT)
	public static record Builder(
		CompactVertexConsumer opaque,
		CompactVertexConsumer translucent
	)
	implements SafeCloseable, VertexConsumerProvider {

		public Builder(CompactVertexFormat format, int initialVertexCount) {
			this(
				new CompactVertexConsumer(initialVertexCount, format),
				new CompactVertexConsumer(initialVertexCount, format)
			);
		}

		@Override
		public VertexConsumer getBuffer(RenderLayer layer) {
			return layer == RenderLayer.getTranslucent() ? this.translucent : this.opaque;
		}

		public LodPasses build(VertexHeap heap) {
			try (ResourceTracker tracker = new ResourceTracker()) {
				Geometry opaque      = tracker.track(Geometry.from(this.opaque,      heap));
				Geometry translucent = tracker.track(Geometry.from(this.translucent, heap));
				tracker.untrackAll(); //success.
				return new LodPasses(opaque, translucent);
			}
		}

		@Override
		public void close() {
			ResourceTracker.closeAll(this.opaque, this.translucent);
		}
	}
}