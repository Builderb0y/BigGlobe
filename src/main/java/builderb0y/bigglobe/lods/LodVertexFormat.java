package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.system.*;

import net.minecraft.util.math.MathHelper;

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
				MemoryUtil.memPutByte(pointer + 4L, (byte)(v0));
				MemoryUtil.memPutByte(pointer + 5L, (byte)(v1));
				MemoryUtil.memPutByte(pointer + 6L, (byte)(v2));
				MemoryUtil.memPutByte(pointer + 7L, (byte)(v3));
			}
		},
		TEXCOORD = new CompactVertexFormatElement.Named("LodVertexFormat.TEXCOORD") {

			@Override
			public void put2f(long pointer, float v0, float v1) {
				MemoryUtil.memPutShort(pointer +  8L, (short)(Math.min((int)(v0 * 65536.0F), 65535)));
				MemoryUtil.memPutShort(pointer + 10L, (short)(Math.min((int)(v1 * 65536.0F), 65535)));
			}
		},
		LMCOORD = new CompactVertexFormatElement.Named("LodVertexFormat.LMCOORD") {

			@Override
			public void put2f(long pointer, float v0, float v1) {
				this.put2i(pointer, (int)(v0 * 256.0F), (int)(v1 * 256.0F));
			}

			@Override
			public void put2i(long pointer, int v0, int v1) {
				int blocklight = MathHelper.clamp(v0, 8, 248);
				int   skylight = MathHelper.clamp(v1, 8, 248);
				MemoryUtil.memPutByte(pointer + 12L, (byte)(blocklight));
				MemoryUtil.memPutByte(pointer + 13L, (byte)(  skylight));
			}
		};

	public static final CompactVertexFormat FORMAT = (
		CompactVertexFormat
		.builder()
		.position(POSITION)
		.color(COLOR)
		.texture(TEXCOORD)
		.lightmap(LMCOORD)
		.byteStride(16)
		.build()
	);
}