package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ARGB;
import builderb0y.bigglobe.rendering.NativeMemory;

@Environment(EnvType.CLIENT)
public class CompactVertexConsumer extends VersionedVertexConsumer {

	public final NativeMemory memory;
	public final CompactVertexFormat format;

	public CompactVertexConsumer(NativeMemory memory, CompactVertexFormat format) {
		super(format.flags);
		this.memory = memory;
		this.format = format;
	}

	public CompactVertexConsumer(long vertexCount, CompactVertexFormat format) {
		this(new NativeMemory(vertexCount * format.byteStride), format);
	}

	public long getCurrentVertexStart() {
		return this.memory.address + this.memory.used - this.format.byteStride;
	}

	@Override
	public void beginVertex() {
		super.beginVertex();
		this.memory.appendEmpty(this.format.byteStride);
	}

	@Override
	public void handlePosition(float x, float y, float z) {
		this.format.putPosition(this.getCurrentVertexStart(), x, y, z);
	}

	@Override
	public void handleColor(int argb) {

		this.handleColor(ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb), ARGB.alpha(argb));
	}

	@Override
	public void handleColor(float red, float green, float blue, float alpha) {
		this.format.putColor(this.getCurrentVertexStart(), red, green, blue, alpha);
	}

	@Override
	public void handleColor(int red, int green, int blue, int alpha) {
		this.format.putColor(this.getCurrentVertexStart(), red, green, blue, alpha);
	}

	@Override
	public void handleTexture(float u, float v) {
		this.format.putTexture(this.getCurrentVertexStart(), u, v);
	}

	@Override
	public void handleOverlay(int u, int v) {
		this.format.putOverlay(this.getCurrentVertexStart(), u, v);
	}

	@Override
	public void handleOverlay(int uv) {
		this.format.putOverlay(this.getCurrentVertexStart(), uv & 0xFFFF, uv >>> 16);
	}

	@Override
	public void handleLight(int blocklight, int skylight) {
		this.format.putLightmap(this.getCurrentVertexStart(), blocklight, skylight);
	}

	@Override
	public void handleLight(int light) {
		this.format.putLightmap(this.getCurrentVertexStart(), light & 0xFFFF, light >>> 16);
	}

	@Override
	public void handleNormal(float nx, float ny, float nz) {
		this.format.putNormal(this.getCurrentVertexStart(), nx, ny, nz);
	}

	@Override
	public void close() {
		this.memory.close();
	}
}