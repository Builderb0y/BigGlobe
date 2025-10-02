package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.rendering.lods.LodPasses.Geometry;
import builderb0y.bigglobe.versions.RenderVersions;

@Environment(EnvType.CLIENT)
public record SidedLodPasses(
	@Nullable Pass opaque,
	@Nullable Pass translucent
)
implements SafeCloseable {

	public @Nullable Pass getPass(boolean translucent) {
		return translucent ? this.translucent : this.opaque;
	}

	@Override
	public void close() {
		ResourceTracker.closeAll(this.opaque, this.translucent);
	}

	public static record Pass(
		@Nullable Geometry posX,
		@Nullable Geometry negX,
		@Nullable Geometry posZ,
		@Nullable Geometry negZ,
		@Nullable Geometry all
	)
	implements SafeCloseable {

		public static Pass from(SidedVertexConsumer consumer, VertexHeap heap) {
			try (ResourceTracker tracker = new ResourceTracker()) {
				Geometry posX = tracker.track(Geometry.from(consumer.posX, heap));
				Geometry negX = tracker.track(Geometry.from(consumer.negX, heap));
				Geometry posZ = tracker.track(Geometry.from(consumer.posZ, heap));
				Geometry negZ = tracker.track(Geometry.from(consumer.negZ, heap));
				Geometry all  = tracker.track(Geometry.from(consumer.all,  heap));
				tracker.untrackAll();
				return new Pass(posX, negX, posZ, negZ, all);
			}
		}

		@Override
		public void close() {
			ResourceTracker.closeAll(this.posX, this.negX, this.posZ, this.negZ, this.all);
		}
	}

	@Environment(EnvType.CLIENT)
	public static record Builder(
		SidedVertexConsumer opaque,
		SidedVertexConsumer translucent
	)
	implements SafeCloseable, VersionedVertexConsumerProvider {

		public Builder(CompactVertexFormat format, int initialVertexCount) {
			this(
				new SidedVertexConsumer(initialVertexCount, format),
				new SidedVertexConsumer(initialVertexCount, format)
			);
		}

		#if MC_VERSION >= MC_1_21_6

			@Override
			public VertexConsumer getBuffer(net.minecraft.client.render.BlockRenderLayer layer) {
				return layer.isTranslucent() ? this.translucent : this.opaque;
			}

		#else

			@Override
			public VertexConsumer getBuffer(RenderLayer layer) {
				return RenderVersions.isTranslucent(layer) ? this.translucent : this.opaque;
			}

		#endif


		public SidedLodPasses build(VertexHeap heap) {
			try (ResourceTracker tracker = new ResourceTracker()) {
				Pass opaque      = tracker.track(Pass.from(this.opaque,      heap));
				Pass translucent = tracker.track(Pass.from(this.translucent, heap));
				tracker.untrackAll(); //success.
				return new SidedLodPasses(opaque, translucent);
			}
		}

		@Override
		public void close() {
			ResourceTracker.closeAll(this.opaque, this.translucent);
		}
	}
}