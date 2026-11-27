package builderb0y.bigglobe.items;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.block.entity.SignBlockEntity;
	import net.minecraft.world.level.block.state.BlockState;

	import builderb0y.bigglobe.util.WorldUtil;
#endif

public class ColoredHangingSignItem extends HangingSignItem {

	public ColoredHangingSignItem(Properties settings, Block hangingSign, Block wallHangingSign) {
		super(hangingSign, wallHangingSign, settings);
	}

	#if MC_VERSION < MC_26_3_0

		@Override
		public boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack itemStack, BlockState placedState) {
			SignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, SignBlockEntity.class);
			if (sign != null) {
				sign.setText(sign.getFrontText().setColor(DyeColor.WHITE), true);
				sign.setText(sign.getBackText().setColor(DyeColor.WHITE), false);
			}
			return super.updateCustomBlockEntityTag(pos, world, player, itemStack, placedState);
		}

	#endif
}