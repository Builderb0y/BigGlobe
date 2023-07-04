package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeBlockTags {

	public static final TagKey<Block>
		MINEABLE_PERCUSSIVE_HAMMER = of("mineable/percussive_hammer"),
		ROCK_BREAKABLE             = of("rock_breakable"),
		END_STONES                 = common("end_stones"),
		END_STONE_SPREADABLE       = of("end_stone_spreadable");

	public static TagKey<Block> of(String name) {
		return TagKey.of(RegistryKeyVersions.block(), BigGlobeMod.modID(name));
	}

	public static TagKey<Block> common(String name) {
		return TagKey.of(RegistryKeyVersions.block(), new Identifier("c", name));
	}
}