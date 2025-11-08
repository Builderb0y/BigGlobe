package builderb0y.bigglobe.entities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public abstract class BigGlobeEntityRenderer<E extends Entity, S extends BigGlobeEntityRenderer.State> extends EntityRenderer<E #if MC_VERSION >= MC_1_21_2 , S #endif> {

	public BigGlobeEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	public abstract S createState();

	public abstract void updateState(E entity, S state, float partialTicks);

	public abstract void doRender(S state, MatrixStack.Entry matrices, VertexConsumer vertexConsumerProvider, Vec3d cameraPosition, int light);

	public abstract RenderLayer getRenderLayer();

	#if MC_VERSION >= MC_1_21_2

		#if MC_VERSION >= MC_1_21_9

			public void render(
				S renderState,
				MatrixStack matrices,
				net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
				net.minecraft.client.render.state.CameraRenderState cameraState
			) {
				super.render(renderState, matrices, queue, cameraState);
				queue.submitCustom(matrices, this.getRenderLayer(), (MatrixStack.Entry entry, VertexConsumer consumer) -> {
					this.doRender(renderState, entry, consumer, cameraState.pos, renderState.light);
				});
			}

		#else

			@Override
			public void render(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
				super.render(state, matrices, vertexConsumers, light);
				this.doRender(state, matrices.peek(), vertexConsumers.getBuffer(this.getRenderLayer()), MinecraftClient.getInstance().gameRenderer.getCamera().getPos(), light);
			}

		#endif

		@Override
		public void updateRenderState(E entity, S state, float tickDelta) {
			super.updateRenderState(entity, state, tickDelta);
			this.updateState(entity, state, tickDelta);
		}

		@Override
		public S createRenderState() {
			return this.createState();
		}

		public static class State extends net.minecraft.client.render.entity.state.EntityRenderState {

		}

	#else

		public final S state = this.createState();

		@Override
		public void render(E entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
			this.state.x = entity.getX();
			this.state.y = entity.getY();
			this.state.z = entity.getZ();
			this.updateState(entity, this.state, tickDelta);
			super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
			this.doRender(this.state, matrices.peek(), vertexConsumers.getBuffer(this.getRenderLayer()), MinecraftClient.getInstance().gameRenderer.getCamera().getPos(), light);
		}

		public static class State {

			public double x, y, z;
		}

	#endif
}