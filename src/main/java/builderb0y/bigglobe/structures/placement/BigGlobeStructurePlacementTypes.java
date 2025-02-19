package builderb0y.bigglobe.structures.placement;

import com.mojang.serialization.MapCodec;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class BigGlobeStructurePlacementTypes {

	public static final StructurePlacementType<ScriptedStructurePlacement> SCRIPTED = register("scripted", ScriptedStructurePlacement.class);

	public static void init() {}

	public static <T extends StructurePlacement> StructurePlacementType<T> register(String name, Class<T> type) {
		MapCodec<T> codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(type);
		return Registry.register(
			Registries.STRUCTURE_PLACEMENT,
			BigGlobeMod.modID(name),
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
		);
	}
}