package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.util.WorldUtil;

#if MC_VERSION >= MC_26_3_0
	import net.minecraft.world.item.StandingAndWallBlockItem;
	import net.minecraft.world.level.block.entity.SignTextSlot;
#else
	import net.minecraft.world.item.SignItem;
#endif

public class ColoredSignItem
extends #if MC_VERSION >= MC_26_3_0 StandingAndWallBlockItem #else SignItem #endif
implements SignColorizer
{

	public final DyeColor color;

	public ColoredSignItem(Properties settings, Block standingBlock, Block wallBlock, DyeColor color) {
		super(standingBlock, wallBlock, #if MC_VERSION >= MC_26_3_0 Direction.DOWN, #endif settings);
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