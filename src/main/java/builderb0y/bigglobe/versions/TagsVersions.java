package builderb0y.bigglobe.versions;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.TagKey;

public class TagsVersions {

	public static TagKey<Block> nylium() { return BlockTags.NYLIUM; }
	public static TagKey<Block> leaves() { return BlockTags.LEAVES; }

	public static TagKey<Fluid> water () { return FluidTags.WATER ; }
	public static TagKey<Fluid> lava  () { return FluidTags.LAVA  ; }
}