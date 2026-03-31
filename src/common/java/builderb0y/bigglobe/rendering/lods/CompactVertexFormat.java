package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CompactVertexFormat {

	public static final int
		FLAG_POSITION = 1 << 0,
		FLAG_COLOR = 1 << 1,
		FLAG_TEXTURE = 1 << 2,
		FLAG_OVERLAY = 1 << 3,
		FLAG_LIGHTMAP = 1 << 4,
		FLAG_NORMAL = 1 << 5;

	public final @Nullable CompactVertexFormatElement
		position,
		color,
		texture,
		overlay,
		lightmap,
		normal;
	public final int
		byteStride;
	public final int
		flags;

	public CompactVertexFormat(Builder builder) {
		this.position   = builder.position;
		this.color      = builder.color;
		this.texture    = builder.texture;
		this.overlay    = builder.overlay;
		this.lightmap   = builder.lightmap;
		this.normal     = builder.normal;
		this.byteStride = builder.byteStride;
		if (this.byteStride < 0) throw new IllegalStateException("Missing stride");

		int flags = 0;
		if (this.position != null) flags |= FLAG_POSITION;
		if (this.color    != null) flags |= FLAG_COLOR;
		if (this.texture  != null) flags |= FLAG_TEXTURE;
		if (this.overlay  != null) flags |= FLAG_OVERLAY;
		if (this.lightmap != null) flags |= FLAG_LIGHTMAP;
		if (this.normal   != null) flags |= FLAG_NORMAL;
		this.flags = flags;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void putPosition(long pointer, float x, float y, float z) {
		if (this.position != null) this.position.put3f(pointer, x, y, z);
	}

	public void putPosition(long pointer, int x, int y, int z) {
		if (this.position != null) this.position.put3i(pointer, x, y, z);
	}

	public void putColor(long pointer, float red, float green, float blue, float alpha) {
		if (this.color != null) this.color.put4f(pointer, red, green, blue, alpha);
	}

	public void putColor(long pointer, int red, int green, int blue, int alpha) {
		if (this.color != null) this.color.put4i(pointer, red, green, blue, alpha);
	}

	public void putTexture(long pointer, float u, float v) {
		if (this.texture != null) this.texture.put2f(pointer, u, v);
	}

	public void putTexture(long pointer, int u, int v) {
		if (this.texture != null) this.texture.put2i(pointer, u, v);
	}

	public void putOverlay(long pointer, float u, float v) {
		if (this.overlay != null) this.overlay.put2f(pointer, u, v);
	}

	public void putOverlay(long pointer, int u, int v) {
		if (this.overlay != null) this.overlay.put2f(pointer, u, v);
	}

	public void putLightmap(long pointer, float blocklight, float skylight) {
		if (this.lightmap != null) this.lightmap.put2f(pointer, blocklight, skylight);
	}

	public void putLightmap(long pointer, int blocklight, int skylight) {
		if (this.lightmap != null) this.lightmap.put2i(pointer, blocklight, skylight);
	}

	public void putNormal(long pointer, float x, float y, float z) {
		if (this.normal != null) this.normal.put3f(pointer, x, y, z);
	}

	public void putNormal(long pointer, int x, int y, int z) {
		if (this.normal != null) this.normal.put3i(pointer, x, y, z);
	}

	public static class Builder {

		public @Nullable CompactVertexFormatElement
			position,
			color,
			texture,
			overlay,
			lightmap,
			normal;
		public int
			byteStride = -1;

		public CompactVertexFormat build() {
			return new CompactVertexFormat(this);
		}

		public Builder position(CompactVertexFormatElement position) {
			this.position = position;
			return this;
		}

		public Builder color(CompactVertexFormatElement color) {
			this.color = color;
			return this;
		}

		public Builder texture(CompactVertexFormatElement texture) {
			this.texture = texture;
			return this;
		}

		public Builder overlay(CompactVertexFormatElement overlay) {
			this.overlay = overlay;
			return this;
		}

		public Builder lightmap(CompactVertexFormatElement lightmap) {
			this.lightmap = lightmap;
			return this;
		}

		public Builder normal(CompactVertexFormatElement normal) {
			this.normal = normal;
			return this;
		}

		public Builder byteStride(int stride) {
			this.byteStride = stride;
			return this;
		}
	}
}