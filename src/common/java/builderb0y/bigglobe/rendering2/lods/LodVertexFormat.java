package builderb0y.bigglobe.rendering2.lods;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;

public class LodVertexFormat {

	public static final int VERTEX_BYTES = 3 * 4, QUAD_BYTES = VERTEX_BYTES * 4;
	public static final @Nullable VertexFormatElement ELEMENT;
	public static final @Nullable VertexFormat FORMAT;
	static {
		done: {
			for (int id = 0; id < VertexFormatElement.MAX_COUNT; id++) {
				if (VertexFormatElement.byId(id) == null) {
					ELEMENT = VertexFormatElement.register(id, 0, VertexFormatElement.Type.UINT, false, 3);
					FORMAT  = VertexFormat.builder().add("bigglobe_rawLodData", ELEMENT).build();
					break done;
				}
			}
			BigGlobeMod.LOGGER.warn("LOD rendering unavailable because too many mods are registering VertexFormat's.");
			ELEMENT = null;
			FORMAT  = null;
		}
	}

	public static void init() {}
}