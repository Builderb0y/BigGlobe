package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.render.VertexConsumer;

@Environment(EnvType.CLIENT)
public class CompactVertexConsumer
#if MC_VERSION >= MC_1_21_0
	implements VertexConsumer, SafeCloseable
#else
	extends net.minecraft.client.render.FixedColorVertexConsumer implements SafeCloseable
#endif
{

	public final NativeMemory memory;
	public final CompactVertexFormat format;
	/**
	used to keep track of which elements have already been specified.
	once all flags in the current format are provided, a new vertex starts.
	*/
	public int flags;
	public int vertexCount;

	public CompactVertexConsumer(NativeMemory memory, CompactVertexFormat format) {
		this.memory = memory;
		this.format = format;
	}

	public CompactVertexConsumer(long vertexCount, CompactVertexFormat format) {
		this(new NativeMemory(vertexCount * format.byteStride), format);
	}

	public void setFlag(int flag) {
		if (this.flags == (this.flags |= flag)) {
			throw new IllegalStateException("Double-specifying element");
		}
	}

	/**
	may be overridden to perform any additional
	actions necessary to prepare for a new vertex.
	*/
	public void beginVertex() {
		#if MC_VERSION >= MC_1_21_0
			if (this.vertexCount++ != 0) this.endVertex();
		#else
			this.vertexCount++;
		#endif
		this.memory.appendEmpty(this.format.byteStride);
	}

	/**
	may be overridden to perform any additional
	actions necessary after a vertex has been fully specified.

	for example, subclasses may track the number of vertices emitted so far,
	and if it's evenly divisible by 4, compute the normal vector from the
	vertex positions instead of using what was provided to {@link #normal}.
	*/
	public void endVertex() {
		int missing = this.format.flags & ~this.flags;
		if (missing != 0) {
			StringBuilder message = new StringBuilder("Missing ");
			if ((missing & CompactVertexFormat.FLAG_POSITION) != 0) message.append("position, ");
			if ((missing & CompactVertexFormat.FLAG_COLOR   ) != 0) message.append("color, ");
			if ((missing & CompactVertexFormat.FLAG_TEXTURE ) != 0) message.append("texture, ");
			if ((missing & CompactVertexFormat.FLAG_OVERLAY ) != 0) message.append("overlay, ");
			if ((missing & CompactVertexFormat.FLAG_LIGHTMAP) != 0) message.append("lightmap, ");
			if ((missing & CompactVertexFormat.FLAG_NORMAL  ) != 0) message.append("normal, ");
			message.setLength(message.length() - 2);
			throw new IllegalStateException(message.toString());
		}
		this.flags = 0;
	}

	public long getCurrentVertexStart() {
		return this.memory.address + this.memory.used - this.format.byteStride;
	}

	#if MC_VERSION >= MC_1_21_0

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			this.beginVertex();
			this.setFlag(CompactVertexFormat.FLAG_POSITION);
			this.format.putPosition(this.getCurrentVertexStart(), x, y, z);
			return this;
		}

	#else

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			this.beginVertex();
			this.setFlag(CompactVertexFormat.FLAG_POSITION);
			this.format.putPosition(this.getCurrentVertexStart(), (float)(x), (float)(y), (float)(z));
			if (this.colorFixed) this.color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha);
			return this;
		}

		@Override
		public void next() {
			this.endVertex();
		}

	#endif

	@Override
	public VertexConsumer color(float red, float green, float blue, float alpha) {
		this.setFlag(CompactVertexFormat.FLAG_COLOR);
		this.format.putColor(this.getCurrentVertexStart(), red, green, blue, alpha);
		return this;
	}

	@Override
	public VertexConsumer color(int red, int green, int blue, int alpha) {
		this.setFlag(CompactVertexFormat.FLAG_COLOR);
		this.format.putColor(this.getCurrentVertexStart(), red, green, blue, alpha);
		return this;
	}

	@Override
	public VertexConsumer texture(float u, float v) {
		this.setFlag(CompactVertexFormat.FLAG_TEXTURE);
		this.format.putTexture(this.getCurrentVertexStart(), u, v);
		return this;
	}

	@Override
	public VertexConsumer overlay(int u, int v) {
		this.setFlag(CompactVertexFormat.FLAG_OVERLAY);
		this.format.putOverlay(this.getCurrentVertexStart(), u, v);
		return this;
	}

	@Override
	public VertexConsumer light(int u, int v) {
		this.setFlag(CompactVertexFormat.FLAG_LIGHTMAP);
		this.format.putLightmap(this.getCurrentVertexStart(), u, v);
		return this;
	}

	@Override
	public VertexConsumer normal(float x, float y, float z) {
		this.setFlag(CompactVertexFormat.FLAG_NORMAL);
		this.format.putNormal(this.getCurrentVertexStart(), x, y, z);
		return this;
	}

	@Override
	public void close() {
		this.memory.close();
	}
}