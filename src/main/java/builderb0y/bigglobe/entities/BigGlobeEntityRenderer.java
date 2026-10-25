package builderb0y.bigglobe.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class BigGlobeEntityRenderer<E extends Entity, S extends BigGlobeEntityRenderer.State> extends EntityRenderer<E> {

	public BigGlobeEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public abstract S createState();

	public abstract void updateState(E entity, S state, float partialTicks);

	public abstract void doRender(S state, PoseStack.Pose matrices, VertexConsumer vertexConsumerProvider, Vec3 cameraPosition, int light);

	public abstract RenderType getRenderLayer();

	@Override
	public net.minecraft.resources.ResourceLocation getTextureLocation(E entity) {
		return null;
	}

	public final S state = this.createState();

	@Override
	public void render(E entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		this.state.x = entity.getX();
		this.state.y = entity.getY();
		this.state.z = entity.getZ();
		this.updateState(entity, this.state, tickDelta);
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		this.doRender(this.state, matrices.last(), vertexConsumers.getBuffer(this.getRenderLayer()), Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), light);
	}

	public static class State {
		public double x, y, z;
	}
}
