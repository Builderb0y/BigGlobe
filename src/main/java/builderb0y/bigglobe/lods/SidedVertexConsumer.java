package builderb0y.bigglobe.lods;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.math.BigGlobeMath;

public class SidedVertexConsumer extends VersionedVertexConsumer {

	public final CompactVertexConsumer posX, negX, posZ, negZ, all;

	public static class Vertex {

		public float x, y, z;
		public int color;
		public float u, v;
		public int overlay;
		public int light;
		public float nx, ny, nz;

		public CompactVertexConsumer selectDelegateFromNormal(SidedVertexConsumer sided) {
			return switch (BigGlobeMath.roundI(this.nx + this.nz * 2.0F)) {
				case -2 -> MathHelper.approximatelyEquals(this.nz, -1.0F) ? sided.negZ : sided.all;
				case -1 -> MathHelper.approximatelyEquals(this.nx, -1.0F) ? sided.negX : sided.all;
				default -> sided.all;
				case +1 -> MathHelper.approximatelyEquals(this.nx, +1.0F) ? sided.posX : sided.all;
				case +2 -> MathHelper.approximatelyEquals(this.nz, +1.0F) ? sided.posZ : sided.all;
			};
		}
	}
	public Vertex[] quad;

	public SidedVertexConsumer(long vertexCount, CompactVertexFormat format) {
		super(format.flags | CompactVertexFormat.FLAG_NORMAL);
		this.posX    = new CompactVertexConsumer(vertexCount, format);
		this.posZ    = new CompactVertexConsumer(vertexCount, format);
		this.negX    = new CompactVertexConsumer(vertexCount, format);
		this.negZ    = new CompactVertexConsumer(vertexCount, format);
		this.all     = new CompactVertexConsumer(vertexCount, format);
		this.quad    = new Vertex[4];
		this.quad[0] = new Vertex();
		this.quad[1] = new Vertex();
		this.quad[2] = new Vertex();
		this.quad[3] = new Vertex();
	}

	@Override
	public void endVertex() {
		if ((this.vertexCount & 3) == 0) {
			CompactVertexConsumer delegate = this.quad[0].selectDelegateFromNormal(this);
			if (delegate != this.all) {
				for (int index = 1; index < 4; index++) {
					CompactVertexConsumer otherDelegate = this.quad[index].selectDelegateFromNormal(this);
					if (otherDelegate != delegate) {
						delegate = this.all;
						break;
					}
				}
			}
			for (Vertex v : this.quad) {
				delegate
				.vertex(v.x, v.y, v.z)
				.color(v.color)
				.texture(v.u, v.v)
				.overlay(v.overlay)
				.light(v.light)
				.normal(v.nx, v.ny, v.nz);
				//don't need to call next() on old versions because that
				//method is a noop on VersionedVertexConsumer anyway.
			}
		}
		super.endVertex();
	}

	@Override
	public void handlePosition(float x, float y, float z) {
		Vertex vertex = this.quad[this.vertexCount & 3];
		vertex.x = x;
		vertex.y = y;
		vertex.z = z;
	}

	@Override
	public void handleColor(float red, float green, float blue, float alpha) {
		this.handleColor((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
	}

	@Override
	public void handleColor(int red, int green, int blue, int alpha) {
		this.handleColor(ColorHelper.getArgb(alpha, red, green, blue));
	}

	@Override
	public void handleColor(int argb) {
		this.quad[this.vertexCount & 3].color = argb;
	}

	@Override
	public void handleTexture(float u, float v) {
		Vertex vertex = this.quad[this.vertexCount & 3];
		vertex.u = u;
		vertex.v = v;
	}

	@Override
	public void handleOverlay(int u, int v) {
		this.handleOverlay(OverlayTexture.packUv(u, v));
	}

	@Override
	public void handleOverlay(int uv) {
		this.quad[this.vertexCount & 2].overlay = uv;
	}

	@Override
	public void handleLight(int blocklight, int skylight) {
		this.handleLight(LightmapTextureManager.pack(blocklight, skylight));
	}

	@Override
	public void handleLight(int light) {
		this.quad[this.vertexCount & 3].light = light;
	}

	@Override
	public void handleNormal(float nx, float ny, float nz) {
		Vertex vertex = this.quad[this.vertexCount & 3];
		vertex.nx = nx;
		vertex.ny = ny;
		vertex.nz = nz;
	}

	@Override
	public void close() {
		ResourceTracker.closeAll(this.posX, this.negX, this.posZ, this.negZ, this.all);
	}
}