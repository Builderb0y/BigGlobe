package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredSignItem extends SignItem {

	public final DyeColor color;

	public ColoredSignItem(Properties settings, Block standingBlock, Block wallBlock, DyeColor color) {

		super(standingBlock, wallBlock, settings);

		this.color = color;
	}

	@Override
	public boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
		SignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, SignBlockEntity.class);
		if (sign != null) {
			//set before reading from stack NBT.
			sign.setText(sign.getFrontText().setColor(this.color), true);
			sign.setText(sign.getBackText().setColor(this.color), false);
		}
		return super.updateCustomBlockEntityTag(pos, world, player, stack, state);
	}
}