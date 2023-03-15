package builderb0y.bigglobe.mixins;

import java.util.ArrayList;
import java.util.List;

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
	as such, ashen netherrack cannot spread by bonemealing regular netherrack.
	2: if there is crimson AND warped nylium adjacent, which one you get is 50/50.
	this chance is not proportional to the *amounts* of each.
	this overwrite fixes both of these issues.
	*/
	@Overwrite
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		List<BlockState> encountered = new ArrayList<>(8);
		BlockPos.Mutable adjacentPos = new BlockPos.Mutable().setY(pos.getY());
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			adjacentPos.setZ(pos.getZ() + offsetZ);
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				if (offsetX == 0 && offsetZ == 0) continue;
				adjacentPos.setX(pos.getX() + offsetX);
				BlockState adjacentState = world.getBlockState(adjacentPos);
				if (adjacentState.isIn(BlockTags.NYLIUM)) {
					encountered.add(adjacentState);
				}
			}
		}
		if (!encountered.isEmpty()) {
			world.setBlockState(pos, encountered.get(random.nextInt(encountered.size())), Block.NOTIFY_ALL);
		}
	}
}