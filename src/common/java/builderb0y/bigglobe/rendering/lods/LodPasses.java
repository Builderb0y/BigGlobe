package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.lods.VertexHeap.Slice;
import builderb0y.bigglobe.util.SafeCloseable;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
		implements SafeCloseable, VersionedVertexConsumerProvider {

		public Builder(CompactVertexFormat format, int initialVertexCount) {
			this(
				new CompactVertexConsumer(initialVertexCount, format),
				new CompactVertexConsumer(initialVertexCount, format)
			);
		}

		@Override
		public VertexConsumer getBuffer(ChunkSectionLayer layer) {
			return layer.sortOnUpload() ? this.translucent : this.opaque;
		}

		public LodPasses build(VertexHeap heap) {
			try (ResourceTracker tracker = new ResourceTracker()) {
				Geometry opaque = tracker.track(Geometry.from(this.opaque, heap));
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