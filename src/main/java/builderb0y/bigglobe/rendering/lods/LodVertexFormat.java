package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.*;

import builderb0y.bigglobe.math.BigGlobeMath;

@Environment(EnvType.CLIENT)
public class LodVertexFormat {

	public static final CompactVertexFormatElement.Named
		POSITION = new CompactVertexFormatElement.Named("LodVertexFormat.POSITION") {

			@Override
			public void put3f(long pointer, float x, float y, float z) {
				int ix = BigGlobeMath.floorI(x * (128.0F / (1 << LodQuadTree.MIN_LEVEL)) + 64.5F);
				int iy = BigGlobeMath.floorI(y * (32768.0F / 4096.0F) + 0.5F);
				int iz = BigGlobeMath.floorI(z * (128.0F / (1 << LodQuadTree.MIN_LEVEL)) + 64.5F);
				MemoryUtil.memPutByte(pointer + 0L, (byte)(ix));
				MemoryUtil.memPutByte(pointer + 1L, (byte)(iz));
				MemoryUtil.memPutShort(pointer + 2L, (short)(iy));
			}
		},
		TEXCOORD = new CompactVertexFormatElement.Named("LodVertexFormat.TEXCOORD") {

			@Override
			public void put2f(long pointer, float v0, float v1) {
				MemoryUtil.memPutShort(pointer + 4L, (short)(Math.min((int)(v0 * 65536.0F), 65535)));
				MemoryUtil.memPutShort(pointer + 6L, (short)(Math.min((int)(v1 * 65536.0F), 65535)));
			}
		},
		COLOR = new CompactVertexFormatElement.Named("LodVertexFormat.COLOR") {

			@Override
			public void put4f(long pointer, float v0, float v1, float v2, float v3) {
				this.put4i(
					pointer,
					Math.min((int)(v0 * 256.0F), 255),
					Math.min((int)(v1 * 256.0F), 255),
					Math.min((int)(v2 * 256.0F), 255),
					Math.min((int)(v3 * 256.0F), 255)
				);
			}

			@Override
			public void put4i(long pointer, int v0, int v1, int v2, int v3) {
				v0 = (v0 >>> 2) & 63;
				v1 = (v1 >>> 2) & 63;
				v2 = (v2 >>> 2) & 63;
				v3 = (v3 >>> 2) & 63;
				int packed = (v3 << 18) | (v2 << 12) | (v1 << 6) | v0;
				MemoryUtil.memPutByte(pointer +  8L, (byte)(packed));
				MemoryUtil.memPutByte(pointer +  9L, (byte)(packed >>>  8));
				MemoryUtil.memPutByte(pointer + 10L, (byte)(packed >>> 16));
			}
		},
		LMCOORD = new CompactVertexFormatElement.Named("LodVertexFormat.LMCOORD") {

			@Override
			public void put2f(long pointer, float v0, float v1) {
				this.put2i(pointer, (int)(v0 * 256.0F), (int)(v1 * 256.0F));
			}

			@Override
			public void put2i(long pointer, int v0, int v1) {
				v0 = (v0 >>> 4) & 15;
				v1 = (v1 >>> 4) & 15;
				int packed = (v1 << 4) | v0;
				MemoryUtil.memPutByte(pointer + 11L, (byte)(packed));
			}
		};

	public static final CompactVertexFormat FORMAT = (
		CompactVertexFormat
		.builder()
		.position(POSITION)
		.color(COLOR)
		.texture(TEXCOORD)
		.lightmap(LMCOORD)
		.byteStride(12)
		.build()
	);
}