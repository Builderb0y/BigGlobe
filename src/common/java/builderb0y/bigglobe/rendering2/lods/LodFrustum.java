package builderb0y.bigglobe.rendering2.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import net.minecraft.world.phys.Vec3;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.bigglobe.versions.RenderVersions;

@Environment(EnvType.CLIENT)
public class LodFrustum {

	public final LodSystem
		system;
	public FrustumIntersection
		jomlFrustum = new FrustumIntersection();
	public double
		x, y, z;
	public float
		nearClippingPlane,
		farClippingPlane,
		generationBuffer;
	public Matrix4f
		modelViewMatrix = new Matrix4f(),
		projectionMatrix = new Matrix4f(),
		modelViewProjectionMatrix = new Matrix4f();

	public LodFrustum(LodSystem system) {
		this.system = system;
	}

	public void setup(LevelExtractionContext context) {
		this.modelViewMatrix.set(context.levelState().cameraRenderState.viewRotationMatrix);
		Vec3 cameraPos = RenderVersions.getCameraPosition(context.camera());
		this.x = cameraPos.x;
		this.y = cameraPos.y;
		this.z = cameraPos.z;

		float vanillaViewDistance = context.gameRenderer().getGameRenderState().optionsRenderState.renderDistance << 4;
		float aboveDifference = (float)(this.y - HeightLimitViewVersions.getMaxY(context.level()));
		if (aboveDifference > 0.0F) {
			vanillaViewDistance = Math.max(vanillaViewDistance, aboveDifference * 0.5F);
		}
		vanillaViewDistance *= this.system.overrides.view_distance_multiplier();
		this.projectionMatrix.set(RenderVersions.projectionMatrix(context));
		this.changeNearFar(
			this.projectionMatrix,
			0.05F,
			context.levelState().cameraRenderState.depthFar,
			this.nearClippingPlane = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.minViewDistance,
			this.farClippingPlane = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.maxViewDistance
		);
		this.generationBuffer = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.generationBufferDistance;
		this.projectionMatrix.mul(this.modelViewMatrix, this.modelViewProjectionMatrix);
		this.jomlFrustum.set(this.modelViewProjectionMatrix, false);
	}

	public void changeNearFar(Matrix4f matrix, float oldNear, float oldFar, float newNear, float newFar) {
		float oldProduct  = oldNear * oldFar;
		float oldSum      = oldNear + oldFar;
		float oldDiff     = oldNear - oldFar;
		float newProduct  = newNear * newFar;
		float newSum      = newNear + newFar;
		float newDiff     = newNear - newFar;
		float productDiff = newProduct / oldProduct;
		float m02 = ((matrix.m03() * oldSum + matrix.m02() * oldDiff) * productDiff - matrix.m03() * newSum) / newDiff;
		float m12 = ((matrix.m13() * oldSum + matrix.m12() * oldDiff) * productDiff - matrix.m13() * newSum) / newDiff;
		float m22 = ((matrix.m23() * oldSum + matrix.m22() * oldDiff) * productDiff - matrix.m23() * newSum) / newDiff;
		float m32 = ((matrix.m33() * oldSum + matrix.m32() * oldDiff) * productDiff - matrix.m33() * newSum) / newDiff;
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