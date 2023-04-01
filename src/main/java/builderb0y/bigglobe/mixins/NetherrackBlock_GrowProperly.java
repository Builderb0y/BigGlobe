package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherrackBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

@Mixin(NetherrackBlock.class)
public class NetherrackBlock_GrowProperly {

	/**
	the default method has 2 flaws:
	1: it's hard-coded for crimson and warped nylium.
	as such, ashen netherrack cannot spread by bonemealing regular netherrack,
	even if it's in BlockTags.NYLIUM (which it is).
	2: if there is crimson AND warped nylium adjacent, which one you get is 50/50.
	this chance is not proportional to the *amounts* of each.
	this overwrite fixes both of these issues.
	*/
	@Overwrite
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockState replacement = null;
		int chance = 0;
		BlockPos.Mutable adjacentPos = new BlockPos.Mutable();
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			adjacentPos.setZ(pos.getZ() + offsetZ);
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				adjacentPos.setX(pos.getX() + offsetX);
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
					adjacentPos.setY(pos.getY() + offsetY);
					BlockState adjacentState = world.getBlockState(adjacentPos);
					if (adjacentState.isIn(BlockTags.NYLIUM) && (chance++ == 0 || random.nextInt(chance) == 0)) {
						replacement = adjacentState;
					}
				}
			}
		}
		if (replacement != null) {
			world.setBlockState(pos, replacement, Block.NOTIFY_ALL);
		}
	}
}