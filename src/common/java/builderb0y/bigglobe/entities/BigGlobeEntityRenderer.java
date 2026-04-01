package builderb0y.bigglobe.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class BigGlobeEntityRenderer<E extends Entity, S extends BigGlobeEntityRenderer.State> extends EntityRenderer<E, S> {

	public BigGlobeEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public abstract S createState();

	public abstract void updateState(E entity, S state, float partialTicks);

	public abstract void doRender(S state, PoseStack.Pose matrices, VertexConsumer vertexConsumerProvider, Vec3 cameraPosition, int light);

	public abstract RenderType getRenderLayer();

	@Override
	public void submit(
		S renderState,
		PoseStack matrices,
		SubmitNodeCollector queue,
		CameraRenderState cameraState
	) {
		super.submit(renderState, matrices, queue, cameraState);
		queue.submitCustomGeometry(
			matrices, this.getRenderLayer(), (PoseStack.Pose entry, VertexConsumer consumer) -> {
				this.doRender(renderState, entry, consumer, cameraState.pos, renderState.lightCoords);
			}
		);
	}

	@Override
	public void extractRenderState(E entity, S state, float tickDelta) {
		super.extractRenderState(entity, state, tickDelta);
		this.updateState(entity, state, tickDelta);
	}

	@Override
	public S createRenderState() {
		return this.createState();
	}

	public static class State extends EntityRenderState {}
}