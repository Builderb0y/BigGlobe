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
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.util.WorldUtil;

public class ColoredHangingSignItem extends HangingSignItem implements SignColorizer {

	public final DyeColor color;

	public ColoredHangingSignItem(Properties settings, Block hangingSign, Block wallHangingSign, DyeColor color) {
		super(hangingSign, wallHangingSign, settings);
		this.color = color;
	}

	#if MC_VERSION >= MC_26_3_0

		@Override
		public void setColors(SignBlockEntity sign) {
			sign.setText(sign.getText(SignTextSlot.FRONT).withColor(this.color), SignTextSlot.FRONT);
			sign.setText(sign.getText(SignTextSlot.BACK).withColor(this.color), SignTextSlot.BACK);
		}

	#else

		@Override
		public void setColors(SignBlockEntity sign) {
			sign.setText(sign.getFrontText().setColor(this.color), true);
			sign.setText(sign.getBackText().setColor(this.color), false);
		}

		@Override
		public boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
			SignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, SignBlockEntity.class);
			if (sign != null) this.setColors(sign);
			return super.updateCustomBlockEntityTag(pos, world, player, stack, state);
		}

	#endif
}