package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SignItem;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.util.WorldUtil;

public class ColoredSignItem extends SignItem {

	public final DyeColor color;

	public ColoredSignItem(Settings settings, Block standingBlock, Block wallBlock, DyeColor color) {
		super(settings, standingBlock, wallBlock);
		this.color = color;
	}

	@Override
	public boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
		SignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, SignBlockEntity.class);
		if (sign != null) {
			//set before reading from stack NBT.
			sign.setText(sign.getFrontText().withColor(this.color), true );
			sign.setText(sign.getBackText ().withColor(this.color), false);
		}
		return super.postPlacement(pos, world, player, stack, state);
	}
}