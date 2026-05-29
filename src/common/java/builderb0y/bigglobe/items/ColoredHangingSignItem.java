package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.util.WorldUtil;

public class ColoredHangingSignItem extends HangingSignItem {

	public final DyeColor color;

	public ColoredHangingSignItem(Properties settings, Block hangingSign, Block wallHangingSign, DyeColor color) {
		super(hangingSign, wallHangingSign, settings);
		this.color = color;
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
		HangingSignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, HangingSignBlockEntity.class);
		if (sign != null) {
			//set before reading from stack NBT.
			sign.setText(sign.getFrontText().setColor(this.color), true);
			sign.setText(sign.getBackText().setColor(this.color), false);
		}
		return super.updateCustomBlockEntityTag(pos, world, player, stack, state);
	}
}