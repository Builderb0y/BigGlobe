package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.versions.EntityVersions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Environment(EnvType.CLIENT)
public class StringEntityRenderer extends BigGlobeEntityRenderer<StringEntity, StringEntityRenderer.State> {

	public static final Identifier TEXTURE = BigGlobeMod.modID("textures/entity/string.png");

	public StringEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public RenderType getRenderLayer() {
		return RenderTypes.entitySolid(TEXTURE);
	}

	@Override
	public void doRender(StringEntityRenderer.State state, PoseStack.Pose matrix, VertexConsumer buffer, Vec3 cameraPosition, int light) {
		Vector3d scratch = new Vector3d();
		class VertexHelper {

			public VertexHelper add(Vector3d pos, float u, float v, Vector3d normal, double normalMultiplier) {
				return this.add(
					pos, u, v, normal, normalMultiplier,
					LightCoordsUtil.pack(
						lerpInt(
							LightCoordsUtil.block(light),
							LightCoordsUtil.block(state.lightC),
							u
						),
						lerpInt(
							LightCoordsUtil.sky(light),
							LightCoordsUtil.sky(state.lightC),
							u
						)
					)
				);
			}

			public VertexHelper add(Vector3d pos, float u, float v, Vector3d normal, double normalMultiplier, int lightLevel) {
				buffer
					.addVertex(matrix.pose(), (float)(pos.x - state.posB.x), (float)(pos.y - state.posB.y), (float)(pos.z - state.posB.z))
					.setColor(255, 255, 255, 255)
					.setUv(u, v)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(lightLevel)
					.setNormal(matrix, (float)(normal.x * normalMultiplier), (float)(normal.y * normalMultiplier), (float)(normal.z * normalMultiplier))

				;
				return this;
			}

			public static int lerpInt(int low, int high, float level) {
				return Math.min(BigGlobeMath.floorI(Interpolator.mixLinear(low, high + 1, level)), high);
			}
		}
		VertexHelper helper = new VertexHelper();
		if (state.nextEntity) {
			BendVector from = new BendVector(state.posA, state.posB, state.posC, state.posD);
			BendVector to = new BendVector(from);
			from.setFrac(0.0D);
			if (!state.prevEntity) {
				helper
				.add(scratch.set(from.position).sub(from.right).add(from.up), 0.0F, 0.5F, from.forward, -1.0D, state.lightB)
				.add(scratch.set(from.position).sub(from.right), 0.0F, 0.625F, from.forward, -1.0D, state.lightB)
				.add(scratch.set(from.position).add(from.right), 0.125F, 0.625F, from.forward, -1.0D, state.lightB)
				.add(scratch.set(from.position).add(from.right).add(from.up), 0.125F, 0.5F, from.forward, -1.0D, state.lightB)
				;
			}
			int segmentCount = Math.max(this.calcSegments(state.posA, state.posB, state.posC), this.calcSegments(state.posB, state.posC, state.posD));
			for (int segment = 0; segment < segmentCount; segment++) {
				to.setFrac(((double)(segment + 1)) / ((double)(segmentCount)));
				float u0 = ((float)(segment)) / ((float)(segmentCount));
				float u1 = ((float)(segment + 1)) / ((float)(segmentCount));

				helper
				.add(scratch.set(from.position).sub(from.right).add(from.up), u0, 0.0F,   from.up,      8.0D)
				.add(scratch.set(from.position).add(from.right).add(from.up), u0, 0.125F, from.up,      8.0D)
				.add(scratch.set(  to.position).add(  to.right).add(  to.up), u1, 0.125F,   to.up,      8.0D)
				.add(scratch.set(  to.position).sub(  to.right).add(  to.up), u1, 0.0F,     to.up,      8.0D)

				.add(scratch.set(from.position).add(from.right).add(from.up), u0, 0.125F, from.right,  16.0D)
				.add(scratch.set(from.position).add(from.right),              u0, 0.25F,  from.right,  16.0D)
				.add(scratch.set(  to.position).add(  to.right),              u1, 0.25F,    to.right,  16.0D)
				.add(scratch.set(  to.position).add(  to.right).add(  to.up), u1, 0.125F,   to.right,  16.0D)

				.add(scratch.set(  to.position).sub(  to.right).add(  to.up), u1, 0.25F,    to.right, -16.0D)
				.add(scratch.set(  to.position).sub(  to.right),              u1, 0.375F,   to.right, -16.0D)
				.add(scratch.set(from.position).sub(from.right),              u0, 0.375F, from.right, -16.0D)
				.add(scratch.set(from.position).sub(from.right).add(from.up), u0, 0.25F,  from.right, -16.0D)

				.add(scratch.set(from.position).add(from.right),              u0, 0.375F, from.up,     -8.0D)
				.add(scratch.set(from.position).sub(from.right),              u0, 0.5F,   from.up,     -8.0D)
				.add(scratch.set(  to.position).sub(  to.right),              u1, 0.5F,     to.up,     -8.0D)
				.add(scratch.set(  to.position).add(  to.right),              u1, 0.375F,   to.up,     -8.0D)
				;

				BendVector tmp = from;
				from = to;
				to = tmp;
			}
			if (!state.nextNextEntity) {
				helper
				.add(scratch.set(from.position).add(from.right).add(from.up), 0.875F, 0.5F,   from.forward, 1.0D, state.lightC)
				.add(scratch.set(from.position).add(from.right),              0.875F, 0.625F, from.forward, 1.0D, state.lightC)
				.add(scratch.set(from.position).sub(from.right),              1.0F,   0.625F, from.forward, 1.0D, state.lightC)
				.add(scratch.set(from.position).sub(from.right).add(from.up), 1.0F,   0.5F,   from.forward, 1.0D, state.lightC)
				;
			}
		}
		else if (!state.prevEntity) {
			Vector3d origin = new Vector3d(state.posB.x, state.posB.y, state.posB.z);
			Vector3d normal = new Vector3d();

			normal.set(0.0D, 0.0D, -1.0D);
			helper
			.add(scratch.set(origin).add(+0.0625D, 0.125D, -0.0625D), 0.0F,   0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.0D,   -0.0625D), 0.0F,   1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.0D,   -0.0625D), 0.125F, 1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.125D, -0.0625D), 0.125F, 0.875F, normal, 1.0D, state.lightB)
			;

			normal.set(-1.0D, 0.0D, 0.0D);
			helper
			.add(scratch.set(origin).add(-0.0625D, 0.125D, -0.0625D), 0.125F, 0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.0D,   -0.0625D), 0.125F, 1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.0D,   +0.0625D), 0.25F,  1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.125D, +0.0625D), 0.25F,  0.875F, normal, 1.0D, state.lightB)
			;

			normal.set(0.0D, 0.0D, 1.0D);
			helper
			.add(scratch.set(origin).add(-0.0625D, 0.125D, +0.0625D), 0.25F,  0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.0D,   +0.0625D), 0.25F,  1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.0D,   +0.0625D), 0.375F, 1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.125D, +0.0625D), 0.375F, 0.875F, normal, 1.0D, state.lightB)
			;

			normal.set(1.0D, 0.0D, 0.0D);
			helper
			.add(scratch.set(origin).add(+0.0625D, 0.125D, +0.0625D), 0.375F, 0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.0D,   +0.0625D), 0.375F, 1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.0D,   -0.0625D), 0.5F,   1.0F,   normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.125D, -0.0625D), 0.5F,   0.875F, normal, 1.0D, state.lightB)
			;

			normal.set(0.0D, 1.0D, 0.0D);
			helper
			.add(scratch.set(origin).add(-0.0625D, 0.125D, -0.0625D), 0.0F,   0.75F,  normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.125D, +0.0625D), 0.0F,   0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.125D, +0.0625D), 0.125F, 0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.125D, -0.0625D), 0.125F, 0.75F,  normal, 1.0D, state.lightB)
			;

			normal.set(0.0D, -1.0D, 0.0D);
			helper
			.add(scratch.set(origin).add(-0.0625D, 0.0D, +0.0625D), 0.125F, 0.75F,  normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(-0.0625D, 0.0D, -0.0625D), 0.125F, 0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.0D, -0.0625D), 0.25F,  0.875F, normal, 1.0D, state.lightB)
			.add(scratch.set(origin).add(+0.0625D, 0.0D, +0.0625D), 0.25F,  0.75F,  normal, 1.0D, state.lightB)
			;
		}
	}

	public static @Nullable Vec3 getPos(Entity entity, float tickDelta) {
		return entity == null ? null : new Vec3(
			Interpolator.mixLinear(EntityVersions.prevX(entity), entity.getX(), tickDelta),
			Interpolator.mixLinear(EntityVersions.prevY(entity), entity.getY(), tickDelta),
			Interpolator.mixLinear(EntityVersions.prevZ(entity), entity.getZ(), tickDelta)
		);
	}

	public int calcSegments(Vec3 a, Vec3 b, Vec3 c) {
		a = a.subtract(b);
		c = c.subtract(b);
		//b = Vec3d.ZERO;
		double div = a.lengthSqr() * c.lengthSqr();
		if (div == 0.0D) return 1;
		double dot = a.dot(c) / Math.sqrt(div);
		dot = Mth.clamp(dot, -1.0D, 1.0D);
		//approximation:
		return Math.min(Math.max(BigGlobeMath.ceilI(Math.sqrt(dot + 1.0D) * 8.0D), 1), 8);
		//exact:
		//double angle = Math.acos(dot);
		//return Math.min(Math.max(BigGlobeMath.ceilI(16.0D - angle * (16.0D / Math.PI)), 1), 8);
	}

	@Override
	public boolean shouldRender(StringEntity entity, Frustum frustum, double x, double y, double z) {
		return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getVisibilityBoundingBox());
	}

	@Override
	public StringEntityRenderer.State createState() {
		return new StringEntityRenderer.State();
	}

	@Override
	public void updateState(StringEntity entity, StringEntityRenderer.State state, float partialTicks) {
		Entity prevEntity = entity.getPrevEntity();
		Entity nextEntity = entity.getNextEntity();
		Entity nextNextEntity = nextEntity instanceof StringEntity string ? string.getNextEntity() : null;

		state.posA = getPos(prevEntity, partialTicks);
		state.posB = getPos(entity, partialTicks);
		state.posC = getPos(nextEntity, partialTicks);
		state.posD = getPos(nextNextEntity, partialTicks);

		if (state.posA == null) state.posA = state.posB;
		if (state.posD == null) state.posD = state.posC;

		state.lightB = this.entityRenderDispatcher.getPackedLightCoords(entity, partialTicks);
		state.lightC = entity.getNextEntity() != null ? this.entityRenderDispatcher.getPackedLightCoords(entity.getNextEntity(), partialTicks) : 0;

		state.prevEntity = prevEntity != null;
		state.nextEntity = nextEntity != null;
		state.nextNextEntity = nextNextEntity != null;
	}

	public static class State extends BigGlobeEntityRenderer.State {

		public Vec3 posA, posB, posC, posD;
		public int lightB, lightC;
		public boolean prevEntity, nextEntity, nextNextEntity;
	}

	public static class BendComponent {

		public final double term1, term2, term3, term4;
		public double value;
		public final double derivativeTerm1, derivativeTerm2, derivativeTerm3;
		public double derivative;

		public BendComponent(double a, double b, double c, double d) {
			this.term1 = Interpolator.cubicTerm1(a, b, c, d);
			this.term2 = Interpolator.cubicTerm2(a, b, c, d);
			this.term3 = Interpolator.cubicTerm3(a, b, c, d);
			this.term4 = Interpolator.cubicTerm4(a, b, c, d);

			this.derivativeTerm1 = Interpolator.cubicDerivativeTerm1(a, b, c, d);
			this.derivativeTerm2 = Interpolator.cubicDerivativeTerm2(a, b, c, d);
			this.derivativeTerm3 = Interpolator.cubicDerivativeTerm3(a, b, c, d);
		}

		public BendComponent(BendComponent that) {
			this.term1 = that.term1;
			this.term2 = that.term2;
			this.term3 = that.term3;
			this.term4 = that.term4;
			this.value = that.value;

			this.derivativeTerm1 = that.derivativeTerm1;
			this.derivativeTerm2 = that.derivativeTerm2;
			this.derivativeTerm3 = that.derivativeTerm3;
			this.derivative = that.derivative;
		}

		public void setFrac(double frac) {
			this.value = Interpolator.combineCubicTerms(this.term1, this.term2, this.term3, this.term4, frac);
			this.derivative = Interpolator.combineCubicDerivativeTerms(this.derivativeTerm1, this.derivativeTerm2, this.derivativeTerm3, frac);
		}
	}

	public static class BendVector {

		public static final Vector3d UP = new Vector3d(0.0D, 1.0D, 0.0D);

		public final BendComponent x, y, z;
		public final Vector3d position, forward, right, up;

		public BendVector(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
			this.x        = new BendComponent(a.x, b.x, c.x, d.x);
			this.y        = new BendComponent(a.y, b.y, c.y, d.y);
			this.z        = new BendComponent(a.z, b.z, c.z, d.z);
			this.position = new Vector3d();
			this.forward  = new Vector3d();
			this.right    = new Vector3d();
			this.up       = new Vector3d();
		}

		public BendVector(BendVector that) {
			this.x        = new BendComponent(that.x);
			this.y        = new BendComponent(that.y);
			this.z        = new BendComponent(that.z);
			this.position = new Vector3d(that.position);
			this.forward  = new Vector3d(that.forward);
			this.right    = new Vector3d(that.right);
			this.up       = new Vector3d(that.up);
		}

		public void setFrac(double frac) {
			this.x.setFrac(frac);
			this.y.setFrac(frac);
			this.z.setFrac(frac);
			this.position.set(this.x.value, this.y.value, this.z.value);
			this.forward.set(this.x.derivative, this.y.derivative, this.z.derivative);
			this.forward.cross(UP, this.right);
			this.right.cross(this.forward, this.up);
			this.forward.normalize();
			this.right.normalize(0.0625D);
			this.up.normalize(0.125D);
		}
	}
}