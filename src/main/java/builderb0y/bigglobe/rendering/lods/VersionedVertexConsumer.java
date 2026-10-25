package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import builderb0y.bigglobe.util.SafeCloseable;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Environment(EnvType.CLIENT)
public abstract class VersionedVertexConsumer

	implements VertexConsumer, SafeCloseable {

	public final int allFlags;
	public int flags;
	public int vertexCount;

	public VersionedVertexConsumer(int allFlags) {
		this.allFlags = allFlags;
	}

	public boolean setFlag(int flag) {
		if ((flag & this.allFlags) != 0) {
			if (this.flags == 0) {
				this.beginVertex();
			}
			if (this.flags == (this.flags |= flag)) {
				throw new IllegalStateException("Double-specifying element");
			}
			return true;
		}
		else {
			return false;
		}
	}

	public void checkEnd() {
		if (this.flags == this.allFlags) {
			this.endVertex();
		}
	}

	public void beginVertex() {
		this.vertexCount++;
	}

	public void endVertex() {
		this.flags = 0;
	}

	public abstract void handlePosition(float x, float y, float z);

	public abstract void handleColor(float red, float green, float blue, float alpha);

	public abstract void handleColor(int red, int green, int blue, int alpha);

	public abstract void handleColor(int argb);

	public abstract void handleTexture(float u, float v);

	public abstract void handleOverlay(int u, int v);

	public abstract void handleOverlay(int uv);

	public abstract void handleLight(int blocklight, int skylight);

	public abstract void handleLight(int light);

	public abstract void handleNormal(float nx, float ny, float nz);

	public VertexConsumer setLineWidth(float width) {
		return this;
	}

	//@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		if (this.setFlag(CompactVertexFormat.FLAG_POSITION)) {
			this.handlePosition(x, y, z);
			this.checkEnd();
		}

		return this;
	}

	@Override
	public VertexConsumer setColor(float red, float green, float blue, float alpha) {
		if (this.setFlag(CompactVertexFormat.FLAG_COLOR)) {
			this.handleColor(red, green, blue, alpha);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		if (this.setFlag(CompactVertexFormat.FLAG_COLOR)) {
			this.handleColor(red, green, blue, alpha);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setColor(int argb) {
		if (this.setFlag(CompactVertexFormat.FLAG_COLOR)) {
			this.handleColor(argb);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		if (this.setFlag(CompactVertexFormat.FLAG_TEXTURE)) {
			this.handleTexture(u, v);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		if (this.setFlag(CompactVertexFormat.FLAG_OVERLAY)) {
			this.handleOverlay(u, v);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setOverlay(int uv) {
		if (this.setFlag(CompactVertexFormat.FLAG_OVERLAY)) {
			this.handleOverlay(uv);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		if (this.setFlag(CompactVertexFormat.FLAG_LIGHTMAP)) {
			this.handleLight(u, v);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setLight(int uv) {
		if (this.setFlag(CompactVertexFormat.FLAG_LIGHTMAP)) {
			this.handleLight(uv);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		if (this.setFlag(CompactVertexFormat.FLAG_NORMAL)) {
			this.handleNormal(x, y, z);
			this.checkEnd();
		}
		return this;
	}
}