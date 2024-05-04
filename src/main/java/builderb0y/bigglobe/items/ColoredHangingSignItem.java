package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.util.WorldUtil;

public class ColoredHangingSignItem extends HangingSignItem {

	public final DyeColor color;

	public ColoredHangingSignItem(Settings settings, Block hangingSign, Block wallHangingSign, DyeColor color) {
		super(hangingSign, wallHangingSign, settings);
		this.color = color;
	}

	@Override
	protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
		HangingSignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, HangingSignBlockEntity.class);
		if (sign != null) {
			//set before reading from stack NBT.
			sign.setText(sign.getFrontText().withColor(this.color), true );
			sign.setText(sign.getBackText ().withColor(this.color), false);
		}
		return super.postPlacement(pos, world, player, stack, state);
	}
}