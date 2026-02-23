package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.bigglobe.versions.RenderVersions;

@Environment(EnvType.CLIENT)
public class LodFrustum {

	public FrustumIntersection
		jomlFrustum = new FrustumIntersection();
	public double
		x, y, z;
	public float
		nearClippingPlane,
		farClippingPlane,
		generationBuffer;
	public Matrix4f
		modelViewMatrix           = new Matrix4f(),
		projectionMatrix          = new Matrix4f(),
		modelViewProjectionMatrix = new Matrix4f();

	public void setup(
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext context
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context
		#endif
	) {
		#if MC_VERSION >= MC_1_21_9
			this.modelViewMatrix.set(context.viewMatrix());
		#elif MC_VERSION >= MC_1_20_5
			this.modelViewMatrix.set(context.positionMatrix());
		#else
			this.modelViewMatrix.set(context.matrixStack().peek().getPositionMatrix());
		#endif

		Vec3d cameraPos = RenderVersions.getCameraPosition(context.camera());
		this.x = cameraPos.x;
		this.y = cameraPos.y;
		this.z = cameraPos.z;

		GameRenderer renderer = context.gameRenderer();

		#if MC_VERSION >= MC_1_21_5
			float vanillaViewDistance = renderer.getViewDistanceBlocks();
		#else
			float vanillaViewDistance = renderer.getViewDistance();
		#endif
		float aboveDifference = (float)(this.y - HeightLimitViewVersions.getMaxY(context.world()));
		if (aboveDifference > 0.0F) {
			vanillaViewDistance = Math.max(vanillaViewDistance, aboveDifference * 0.25F);
		}

		this.projectionMatrix.set(RenderVersions.projectionMatrix(context));
		float minMultiplier = BigGlobeConfig.INSTANCE.get().lodRendering.minViewDistance;
		float maxMultiplier = BigGlobeConfig.INSTANCE.get().lodRendering.maxViewDistance;
		float generationMultiplier = BigGlobeConfig.INSTANCE.get().lodRendering.generationBufferDistance;
		vanillaViewDistance = Math.min(vanillaViewDistance, 60_000_000.0F / generationMultiplier);
		this.changeNearFar(
			this.projectionMatrix,
			0.05F,
			renderer.getFarPlaneDistance(),
			this.nearClippingPlane = vanillaViewDistance * minMultiplier,
			this.farClippingPlane = vanillaViewDistance * maxMultiplier
		);
		this.generationBuffer = vanillaViewDistance * generationMultiplier;
		this.projectionMatrix.mul(this.modelViewMatrix, this.modelViewProjectionMatrix);
		this.jomlFrustum.set(this.modelViewProjectionMatrix, false);
	}

	public void changeNearFar(Matrix4f matrix, float oldNear, float oldFar, float newNear, float newFar) {
		float denominator = (newNear - newFar) * oldFar * oldNear;
		float l = (newFar * newNear * (oldNear - oldFar)) / denominator;
		float r = (newFar * newNear * (oldNear + oldFar)) / denominator - (newNear + newFar) / (newNear - newFar);
		float m02 = r * matrix.m03() + l * matrix.m02();
		float m12 = r * matrix.m13() + l * matrix.m12();
		float m22 = r * matrix.m23() + l * matrix.m22();
		float m32 = r * matrix.m33() + l * matrix.m32();
		matrix.m02(m02).m12(m12).m22(m22).m32(m32);
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