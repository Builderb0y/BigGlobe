package builderb0y.bigglobe.lods;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

@Environment(EnvType.CLIENT)
public class LodFrustum {

	public FrustumIntersection
		jomlFrustum = new FrustumIntersection();
	public double
		x, y, z;
	public float
		nearClippingPlane,
		farClippingPlane;
	public Matrix4f
		modelViewMatrix = new Matrix4f(),
		vanillaProjectionMatrix = new Matrix4f(),
		lodProjectionMatrix = new Matrix4f(),
		frustumMatrix = new Matrix4f();

	public void setup(WorldRenderContext context) {
		#if MC_VERSION >= MC_1_20_5
			this.modelViewMatrix.set(context.positionMatrix());
		#else
			this.modelViewMatrix.set(context.matrixStack().peek().getPositionMatrix());
		#endif

		Vec3d cameraPos = context.camera().getPos();
		this.x = cameraPos.x;
		this.y = cameraPos.y;
		this.z = cameraPos.z;
		this.vanillaProjectionMatrix.set(context.projectionMatrix());

		/**
		mostly a copy-paste of {@link GameRenderer#getBasicProjectionMatrix(float)},
		but with a different near/far clipping plane.
		*/
		this.lodProjectionMatrix.identity();
		GameRenderer renderer = context.gameRenderer();
		Window window = MinecraftClient.getInstance().getWindow();
		if (renderer.zoom != 1.0F) {
			this.lodProjectionMatrix.translate(renderer.zoomX, -renderer.zoomY, 0.0F);
			this.lodProjectionMatrix.scale(renderer.zoom, renderer.zoom, 1.0F);
		}
		#if MC_VERSION >= MC_1_21_5
			float vanillaViewDistance = renderer.getViewDistanceBlocks();
		#else
			float vanillaViewDistance = renderer.getViewDistance();
		#endif
		float aboveDifference = (float)(context.camera().getPos().y - HeightLimitViewVersions.getMaxY(context.world()));
		if (aboveDifference > 0.0F) {
			vanillaViewDistance = Math.max(vanillaViewDistance, aboveDifference);
		}
		this.lodProjectionMatrix.perspective(
			#if MC_VERSION >= MC_1_21_5
				renderer.getFov(context.camera(), context.tickCounter(). getTickProgress(false), true) * (float)(Math.PI / 180.0F),
			#elif MC_VERSION >= MC_1_21_2
				renderer.getFov(context.camera(), context.tickCounter(). getTickDelta(false), true) * (float)(Math.PI / 180.0F),
			#elif MC_VERSION >= MC_1_21_1
				(float)(renderer.getFov(context.camera(), context.tickCounter().getTickDelta(false), true) * (Math.PI / 180.0D)),
			#else
				(float)(renderer.getFov(context.camera(), context.tickDelta(), true) * (Math.PI / 180.0D)),
			#endif
			((float)(window.getFramebufferWidth())) / ((float)(window.getFramebufferHeight())),
			this.nearClippingPlane = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.minViewDistance,
			this.farClippingPlane = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.maxViewDistance
		);
		context.projectionMatrix().set(this.lodProjectionMatrix);

		this.lodProjectionMatrix.mul(this.modelViewMatrix, this.frustumMatrix);
		this.jomlFrustum.set(this.frustumMatrix, false);
	}

	public void restore(WorldRenderContext context) {
		context.projectionMatrix().set(this.vanillaProjectionMatrix);
	}

	public Boolean test(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		int test = this.jomlFrustum.intersectAab(
			(float)(minX - this.x),
			(float)(minY - this.y),
			(float)(minZ - this.z),
			(float)(maxX - this.x),
			(float)(maxY - this.y),
			(float)(maxZ - this.z)
		);
		if (test == FrustumIntersection.INSIDE) return Boolean.TRUE;
		if (test != FrustumIntersection.INTERSECT) return Boolean.FALSE;
		return null;
	}
}