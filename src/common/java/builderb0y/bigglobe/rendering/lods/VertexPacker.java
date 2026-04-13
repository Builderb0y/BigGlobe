package builderb0y.bigglobe.rendering.lods;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.ARGB;

@Environment(EnvType.CLIENT)
public class VertexPacker implements VertexConsumer {

	public static final byte
		FLAG_POS   = 1 << 2,
		FLAG_UV    = 1 << 3,
		FLAG_COLOR = 1 << 4,
		FLAG_LIGHT = 1 << 5,
		FLAG_ALL   = FLAG_POS | FLAG_UV | FLAG_COLOR | FLAG_LIGHT;

	public float
		x0, x1, x2, x3,
		y0, y1, y2, y3,
		z0, z1, z2, z3,
		u0, u1, u2, u3,
		v0, v1, v2, v3;
	public byte
		r0, r1, r2, r3, //red
		g0, g1, g2, g3, //green
		b0, b1, b2, b3, //blue
		a0, a1, a2, a3, //alpha
		t0, t1, t2, t3, //torch light
		s0, s1, s2, s3; //sky light
	public byte
		state;

	public final ChunkSectionLayer layer;
	public final QuadPacker<?> output;

	public VertexPacker(ChunkSectionLayer layer, QuadPacker<?> output) {
		this.layer = layer;
		this.output = output;
	}

	public VertexConsumer updateState(byte flag) {
		int state = this.state | flag;
		if ((state & FLAG_ALL) == FLAG_ALL) {
			state = (state & 3) + 1;
			if (state == 4) {
				state = 0;
				this.output.accept(this);
			}
		}
		this.state = (byte)(state);
		return this;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		switch (this.state & 3) {
			case 0 -> {
				this.x0 = x;
				this.y0 = y;
				this.z0 = z;
			}
			case 1 -> {
				this.x1 = x;
				this.y1 = y;
				this.z1 = z;
			}
			case 2 -> {
				this.x2 = x;
				this.y2 = y;
				this.z2 = z;
			}
			case 3 -> {
				this.x3 = x;
				this.y3 = y;
				this.z3 = z;
			}
		}
		return this.updateState(FLAG_POS);
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		switch (this.state & 3) {
			case 0 -> {
				this.r0 = (byte)(r);
				this.g0 = (byte)(g);
				this.b0 = (byte)(b);
				this.a0 = (byte)(a);
			}
			case 1 -> {
				this.r1 = (byte)(r);
				this.g1 = (byte)(g);
				this.b1 = (byte)(b);
				this.a1 = (byte)(a);
			}
			case 2 -> {
				this.r2 = (byte)(r);
				this.g2 = (byte)(g);
				this.b2 = (byte)(b);
				this.a2 = (byte)(a);
			}
			case 3 -> {
				this.r3 = (byte)(r);
				this.g3 = (byte)(g);
				this.b3 = (byte)(b);
				this.a3 = (byte)(a);
			}
		}
		return this.updateState(FLAG_COLOR);
	}

	@Override
	public VertexConsumer setColor(int color) {
		return this.setColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		switch (this.state & 3) {
			case 0 -> {
				this.u0 = u;
				this.v0 = v;
			}
			case 1 -> {
				this.u1 = u;
				this.v1 = v;
			}
			case 2 -> {
				this.u2 = u;
				this.v2 = v;
			}
			case 3 -> {
				this.u3 = u;
				this.v3 = v;
			}
		}
		return this.updateState(FLAG_UV);
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer setUv2(int t, int s) {
		switch (this.state & 3) {
			case 0 -> {
				this.t0 = (byte)(t);
				this.s0 = (byte)(s);
			}
			case 1 -> {
				this.t1 = (byte)(t);
				this.s1 = (byte)(s);
			}
			case 2 -> {
				this.t2 = (byte)(t);
				this.s2 = (byte)(s);
			}
			case 3 -> {
				this.t3 = (byte)(t);
				this.s3 = (byte)(s);
			}
		}
		return this.updateState(FLAG_LIGHT);
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		return this;
	}
}