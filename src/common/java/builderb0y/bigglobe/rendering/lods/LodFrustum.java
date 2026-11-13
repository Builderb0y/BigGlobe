package builderb0y.bigglobe.rendering.lods;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import net.minecraft.world.phys.Vec3;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.CommonState;
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

	public void setup(LevelTerrainRenderContext context) {
		this.modelViewMatrix.set(CommonState.modelViewMatrix);
		#if MC_VERSION >= MC_26_2_0
			Vec3 cameraPos = context.levelState().cameraRenderState.pos;
			float vanillaViewDistance = context.gameRenderer().gameRenderState().optionsRenderState.renderDistance << 4;
		#else
			Vec3 cameraPos = RenderVersions.getCameraPosition(context.gameRenderer().getMainCamera());
			float vanillaViewDistance = context.gameRenderer().getGameRenderState().optionsRenderState.renderDistance << 4;
		#endif
		this.x = cameraPos.x;
		this.y = cameraPos.y;
		this.z = cameraPos.z;

		float aboveDifference = (float)(this.y - this.system.params.maxY);
		if (aboveDifference > 0.0F) {
			vanillaViewDistance = Math.max(vanillaViewDistance, aboveDifference * 0.5F);
		}
		vanillaViewDistance *= this.system.overrides.view_distance_multiplier();
		this.projectionMatrix.set(CommonState.projectionMatrix);
		this.changeNearFar(
			this.projectionMatrix,
			0.05F,
			context.levelState().cameraRenderState.depthFar,
			this.nearClippingPlane = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.minViewDistance,
			this.farClippingPlane = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.maxViewDistance,
			#if MC_VERSION >= MC_26_2_0
				RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
			#else
				false
			#endif
		);
		this.generationBuffer = vanillaViewDistance * BigGlobeConfig.INSTANCE.get().lodRendering.generationBufferDistance;
		this.projectionMatrix.mul(this.modelViewMatrix, this.modelViewProjectionMatrix);
		this.jomlFrustum.set(this.modelViewProjectionMatrix, false);
	}

	public void changeNearFar(Matrix4f matrix, float oldNear, float oldFar, float newNear, float newFar, boolean zZeroToOne) {
		#if MC_VERSION >= MC_26_2_0
			float tmp = oldNear;
			oldNear = oldFar;
			oldFar = tmp;

			tmp = newNear;
			newNear = newFar;
			newFar = tmp;
		#endif
		float newProduct = newNear * newFar;
		float oldProduct = oldNear * oldFar;
		float oldDiff = oldNear - oldFar;
		float newDiff = newNear - newFar;
		float rcpNewDiff = 1.0F / newDiff;
		float m02, m12, m22, m32;
		if (zZeroToOne) {
			float rcpOldNear = 1.0F / oldNear;
			float oldDiffProduct = oldDiff / oldProduct;
			m02 = (newProduct * (matrix.m02() * oldDiffProduct + matrix.m03() * rcpOldNear) - matrix.m03() * newFar) * rcpNewDiff;
			m12 = (newProduct * (matrix.m12() * oldDiffProduct + matrix.m13() * rcpOldNear) - matrix.m13() * newFar) * rcpNewDiff;
			m22 = (newProduct * (matrix.m22() * oldDiffProduct + matrix.m23() * rcpOldNear) - matrix.m23() * newFar) * rcpNewDiff;
			m32 = (newProduct * (matrix.m32() * oldDiffProduct + matrix.m33() * rcpOldNear) - matrix.m33() * newFar) * rcpNewDiff;
		}
		else {
			float oldSum = oldNear + oldFar;
			float newSum = newNear + newFar;
			float productQuotient = newProduct / oldProduct;
			m02 = ((matrix.m03() * oldSum + matrix.m02() * oldDiff) * productQuotient - matrix.m03() * newSum) * rcpNewDiff;
			m12 = ((matrix.m13() * oldSum + matrix.m12() * oldDiff) * productQuotient - matrix.m13() * newSum) * rcpNewDiff;
			m22 = ((matrix.m23() * oldSum + matrix.m22() * oldDiff) * productQuotient - matrix.m23() * newSum) * rcpNewDiff;
			m32 = ((matrix.m33() * oldSum + matrix.m32() * oldDiff) * productQuotient - matrix.m33() * newSum) * rcpNewDiff;
		}
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