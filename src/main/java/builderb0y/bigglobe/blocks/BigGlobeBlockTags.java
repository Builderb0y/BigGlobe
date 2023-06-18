package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeBlockTags {

	public static final TagKey<Block>
		AMETHYST_BLOCKS            = of("amethyst_blocks"),
		AMETHYST_BUDS              = of("amethyst_buds"),
		MINEABLE_PERCUSSIVE_HAMMER = of("mineable/percussive_hammer"),
		ROCK_BREAKABLE             = of("rock_breakable"),
		REPLACEABLE_PLANTS         = of("replaceable_plants"),
		END_STONES                 = common("end_stones"),
		END_STONE_SPREADABLE       = of("end_stone_spreadable");

	public static TagKey<Block> of(String name) {
		return TagKey.of(RegistryKeys.BLOCK, BigGlobeMod.modID(name));
	}

	public static TagKey<Block> common(String name) {
		return TagKey.of(RegistryKeys.BLOCK, new Identifier("c", name));
	}
}