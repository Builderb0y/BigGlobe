package builderb0y.bigglobe.rendering.lods;

import java.nio.ByteOrder;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;

import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public abstract class QuadPacker<T_Mesh extends SafeCloseable> implements Consumer<QuadView>, SafeCloseable {

	public final QuadSorter sorter;

	public QuadPacker(QuadSorter sorter) {
		this.sorter = sorter;
	}

	public abstract T_Mesh build(Supplier<String> name);

	@Override
	public void accept(QuadView view) {
		ByteOrder order = ByteOrder.nativeOrder();
		NativeMemory output = this.sorter.getOutput(view);
		for (int vertex = 0; vertex < 4; vertex++) {
			int color = view.color(vertex);
			int light = view.lightmap(vertex);
			output.appendInt(this.encodePosition(view.x(vertex), view.y(vertex), view.z(vertex)), order);
			output.appendInt(this.encodeTexcoord(view.u(vertex), view.v(vertex)), order);
			output.appendInt(this.packColorLight(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color), LightCoordsUtil.smoothBlock(light), LightCoordsUtil.smoothSky(light)), order);
		}
	}

	public void accept(VertexPacker view) {
		ByteOrder order = ByteOrder.nativeOrder();
		NativeMemory output = this.sorter.getOutput(view);

		output.appendInt(this.encodePosition(view.x0, view.y0, view.z0), order);
		output.appendInt(this.encodeTexcoord(view.u0, view.v0), order);
		output.appendInt(this.packColorLight(view.r0, view.g0, view.b0, view.a0, view.t0, view.s0), order);

		output.appendInt(this.encodePosition(view.x1, view.y1, view.z1), order);
		output.appendInt(this.encodeTexcoord(view.u1, view.v1), order);
		output.appendInt(this.packColorLight(view.r1, view.g1, view.b1, view.a1, view.t1, view.s1), order);

		output.appendInt(this.encodePosition(view.x2, view.y2, view.z2), order);
		output.appendInt(this.encodeTexcoord(view.u2, view.v2), order);
		output.appendInt(this.packColorLight(view.r2, view.g2, view.b2, view.a2, view.t2, view.s2), order);

		output.appendInt(this.encodePosition(view.x3, view.y3, view.z3), order);
		output.appendInt(this.encodeTexcoord(view.u3, view.v3), order);
		output.appendInt(this.packColorLight(view.r3, view.g3, view.b3, view.a3, view.t3, view.s3), order);
	}

	public abstract int encodePosition(float x, float y, float z);

	public int encodeTexcoord(float u, float v) {
		int packedU = Mth.clamp((int)(u * 65536.0F), 0, 65535);
		int packedV = Mth.clamp((int)(v * 65536.0F), 0, 65535);
		return packedU | (packedV << 16);
	}

	public int packColorLight(int r, int g, int b, int a, int blocklight, int skylight) {
		r = (r >> 2) & 63;
		g = (g >> 2) & 63;
		b = (b >> 2) & 63;
		a = (a >> 2) & 63;
		blocklight = (blocklight >> 4) & 15;
		skylight = (skylight >> 4) & 15;
		return r | (g << 6) | (b << 12) | (a << 18) | (blocklight << 24) | (skylight << 28);
	}

	@Override
	public void close() {
		this.sorter.close();
	}
}