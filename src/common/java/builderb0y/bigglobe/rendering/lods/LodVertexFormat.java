package builderb0y.bigglobe.rendering.lods;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;

#if MC_VERSION >= MC_26_2_0
	import com.mojang.blaze3d.GpuFormat;
#endif

@Environment(EnvType.CLIENT)
public class LodVertexFormat {

	public static final int VERTEX_BYTES = 3 * 4, QUAD_BYTES = VERTEX_BYTES * 4;
	public static final @Nullable VertexFormatElement ELEMENT;
	public static final @Nullable VertexFormat FORMAT;
	static {
		#if MC_VERSION >= MC_26_2_0
			FORMAT = VertexFormat.builder(0).addAttribute("bigglobe_rawLodData", GpuFormat.RGB32_UINT).build();
			ELEMENT = FORMAT.getElement("bigglobe_rawLodData");
		#else
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
		#endif
	}

	public static void init() {}
}