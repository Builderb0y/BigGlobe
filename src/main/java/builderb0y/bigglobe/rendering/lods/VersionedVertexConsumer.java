package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.render.VertexConsumer;

import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public abstract class VersionedVertexConsumer
#if MC_VERSION >= MC_1_21_0
	implements VertexConsumer, SafeCloseable
#else
	extends net.minecraft.client.render.FixedColorVertexConsumer implements SafeCloseable
#endif
{
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

	#if MC_VERSION >= MC_1_21_0 @Override #endif
	public VertexConsumer vertex(float x, float y, float z) {
		if (this.setFlag(CompactVertexFormat.FLAG_POSITION)) {
			this.handlePosition(x, y, z);
			this.checkEnd();
		}
		#if MC_VERSION < MC_1_21_0
			if (this.colorFixed) this.color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha);
		#endif
		return this;
	}

	#if MC_VERSION < MC_1_21_0

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			return this.vertex((float)(x), (float)(y), (float)(z));
		}

		@Override
		public void next() {}

	#endif

	@Override
	public VertexConsumer color(float red, float green, float blue, float alpha) {
		if (this.setFlag(CompactVertexFormat.FLAG_COLOR)) {
			this.handleColor(red, green, blue, alpha);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		if (this.setFlag(CompactVertexFormat.FLAG_COLOR)) {
			this.handleColor(red, green, blue, alpha);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer color(int argb) {
		if (this.setFlag(CompactVertexFormat.FLAG_COLOR)) {
			this.handleColor(argb);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer texture(float u, float v) {
		if (this.setFlag(CompactVertexFormat.FLAG_TEXTURE)) {
			this.handleTexture(u, v);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer overlay(int u, int v) {
		if (this.setFlag(CompactVertexFormat.FLAG_OVERLAY)) {
			this.handleOverlay(u, v);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer overlay(int uv) {
		if (this.setFlag(CompactVertexFormat.FLAG_OVERLAY)) {
			this.handleOverlay(uv);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer light(int u, int v) {
		if (this.setFlag(CompactVertexFormat.FLAG_LIGHTMAP)) {
			this.handleLight(u, v);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer light(int uv) {
		if (this.setFlag(CompactVertexFormat.FLAG_LIGHTMAP)) {
			this.handleLight(uv);
			this.checkEnd();
		}
		return this;
	}

	@Override
	public VertexConsumer normal(float x, float y, float z) {
		if (this.setFlag(CompactVertexFormat.FLAG_NORMAL)) {
			this.handleNormal(x, y, z);
			this.checkEnd();
		}
		return this;
	}
}