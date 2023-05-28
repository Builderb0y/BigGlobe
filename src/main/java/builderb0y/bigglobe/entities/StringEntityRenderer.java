package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.Interpolator;

@Environment(EnvType.CLIENT)
public class StringEntityRenderer extends EntityRenderer<StringEntity> {

	public static final Identifier TEXTURE = BigGlobeMod.modID("textures/entity/string.png");

	public StringEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(
		StringEntity entity,
		float yaw,
		float tickDelta,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		int light
	) {
		Entity next = entity.getNextEntity();
		if (next != null) {
			Vec3d a = getPos(entity.getPrevEntity(), tickDelta);
			Vec3d b = getPos(entity, tickDelta);
			Vec3d c = getPos(next, tickDelta);
			Vec3d d = getPos(next instanceof StringEntity string ? string.getNextEntity() : null, tickDelta);
			if (a == null) a = b;
			if (d == null) d = c;
			BendVector from = new BendVector(a, b, c, d);
			from.setFrac(0.0D);
			BendVector to = new BendVector(from);
			Vector3d scratch = new Vector3d();
			MatrixStack.Entry matrix = matrices.peek();
			VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(this.getTexture(entity)));
			class VertexHelper {

				public VertexHelper add(Vector3d pos, float u, float v, Vector3d normal, double normalMultiplier) {
					buffer
					.vertex(matrix.getPositionMatrix(), (float)(pos.x - b.x), (float)(pos.y - b.y), (float)(pos.z - b.z))
					.color(255, 255, 255, 255)
					.texture(u, v)
					.overlay(OverlayTexture.DEFAULT_UV)
					.light(light)
					.normal(matrix.getNormalMatrix(), (float)(normal.x * normalMultiplier), (float)(normal.y * normalMultiplier), (float)(normal.z * normalMultiplier))
					.next();
					return this;
				}
			}
			VertexHelper helper = new VertexHelper();
			if (!(entity.getPrevEntity() instanceof StringEntity)) {
				helper
				.add(scratch.set(from.position).sub(from.right).add(from.up), 0.0F,   0.5F,   from.forward, -1.0D)
				.add(scratch.set(from.position).sub(from.right),              0.0F,   0.625F, from.forward, -1.0D)
				.add(scratch.set(from.position).add(from.right),              0.125F, 0.625F, from.forward, -1.0D)
				.add(scratch.set(from.position).add(from.right).add(from.up), 0.125F, 0.5F,   from.forward, -1.0D)
				;
			}
			//todo: dynamic segment count based on bendyness.
			for (int segment = 0; segment < 8; segment++) {
				to.setFrac(segment * 0.125D + 0.125D);
				float u0 = segment * 0.125F;
				float u1 = u0 + 0.125F;

				helper
				.add(scratch.set(from.position).sub(from.right).add(from.up), u0, 0.0F,   from.up,     8.0D)
				.add(scratch.set(from.position).add(from.right).add(from.up), u0, 0.125F, from.up,     8.0D)
				.add(scratch.set(  to.position).add(  to.right).add(  to.up), u1, 0.125F,   to.up,     8.0D)
				.add(scratch.set(  to.position).sub(  to.right).add(  to.up), u1, 0.0F,     to.up,     8.0D)

				.add(scratch.set(from.position).add(from.right).add(from.up), u0, 0.125F, from.right, 16.0D)
				.add(scratch.set(from.position).add(from.right),              u0, 0.25F,  from.right, 16.0D)
				.add(scratch.set(  to.position).add(  to.right),              u1, 0.25F,    to.right, 16.0D)
				.add(scratch.set(  to.position).add(  to.right).add(  to.up), u1, 0.125F,   to.right, 16.0D)

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
			if (!(next instanceof StringEntity string) || !(string.getNextEntity() instanceof StringEntity)) {
				helper
				.add(scratch.set(from.position).add(from.right).add(from.up), 0.875F, 0.5F,   from.forward, 1.0D)
				.add(scratch.set(from.position).add(from.right),              0.875F, 0.625F, from.forward, 1.0D)
				.add(scratch.set(from.position).sub(from.right),              1.0F,   0.625F, from.forward, 1.0D)
				.add(scratch.set(from.position).sub(from.right).add(from.up), 1.0F,   0.5F,   from.forward, 1.0D)
				;
			}
		}
	}

	public static @Nullable Vec3d getPos(Entity entity, float tickDelta) {
		return entity == null ? null : new Vec3d(
			Interpolator.mixLinear(entity.prevX, entity.getX(), tickDelta),
			Interpolator.mixLinear(entity.prevY, entity.getY(), tickDelta),
			Interpolator.mixLinear(entity.prevZ, entity.getZ(), tickDelta)
		);
	}

	@Override
	public Identifier getTexture(StringEntity entity) {
		return TEXTURE;
	}

	public static class BendComponent {

		public double term1, term2, term3, term4;
		public double value;
		public double derivativeTerm1, derivativeTerm2, derivativeTerm3;
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
			this.derivative      = that.derivative;
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

		public BendVector(Vec3d a, Vec3d b, Vec3d c, Vec3d d) {
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