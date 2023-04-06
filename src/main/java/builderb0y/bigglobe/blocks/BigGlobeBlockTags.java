package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeBlockTags {

	public static final TagKey<Block>
		AMETHYST_BLOCKS            = of("amethyst_blocks"),
		AMETHYST_BUDS              = of("amethyst_buds"),
		MINEABLE_PERCUSSIVE_HAMMER = of("mineable/percussive_hammer");

	public static TagKey<Block> of(String name) {
		return TagKey.of(RegistryKeys.BLOCK, BigGlobeMod.modID(name));
	}
}