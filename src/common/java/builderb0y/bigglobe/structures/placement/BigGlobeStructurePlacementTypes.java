package builderb0y.bigglobe.structures.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
#endif

public class BigGlobeStructurePlacementTypes {

	public static final #if MC_VERSION >= MC_26_3_0 MapCodec #else StructurePlacementType #endif <ScriptedStructurePlacement> SCRIPT = register("script", ScriptedStructurePlacement.class);

	public static void init() {}

	public static <T extends StructurePlacement> #if MC_VERSION >= MC_26_3_0 MapCodec #else StructurePlacementType #endif <T> register(String name, Class<T> type) {
		MapCodec<T> codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(type);
		return Registry.register(
			BuiltInRegistries.STRUCTURE_PLACEMENT,
			BigGlobeMod.modID(name),
			#if MC_VERSION >= MC_26_3_0
				codec
			#else
				new StructurePlacementType<T>() {

					@Override
					public MapCodec<T> codec() {
						return codec;
					}

					@Override
					public String toString() {
						return "StructurePlacementType: bigglobe:" + name;
					}
				}
			#endif
		);
	}
}