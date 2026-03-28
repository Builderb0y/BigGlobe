package builderb0y.bigglobe.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherrackBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NetherrackBlock.class)
public class NetherrackBlock_GrowProperly {

	/**
	the vanilla method has 2 flaws:
	1: it's hard-coded for crimson and warped nylium.
	as such, ashen netherrack cannot spread by bonemealing regular netherrack,
	even if it's in BlockTags.NYLIUM (which it is).
	2: if there is crimson AND warped nylium adjacent, which one you get is 50/50.
	this chance is not proportional to the *amounts* of each.
	this overwrite fixes both of these issues.

	@author Builderb0y
	@reason vanilla logic is hard-coded for vanilla blocks.
	*/
	@Overwrite
	public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
		BlockState replacement = null;
		int chance = 0;
		BlockPos.MutableBlockPos adjacentPos = new BlockPos.MutableBlockPos();
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			adjacentPos.setZ(pos.getZ() + offsetZ);
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				adjacentPos.setX(pos.getX() + offsetX);
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
					adjacentPos.setY(pos.getY() + offsetY);
					BlockState adjacentState = world.getBlockState(adjacentPos);
					if (adjacentState.is(BlockTags.NYLIUM) && (chance++ == 0 || random.nextInt(chance) == 0)) {
						replacement = adjacentState;
					}
				}
			}
		}
		if (replacement != null) {
			world.setBlock(pos, replacement, Block.UPDATE_ALL);
		}
	}
}