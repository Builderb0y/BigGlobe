package builderb0y.bigglobe.items;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

#if MC_VERSION >= MC_26_3_0
	import net.minecraft.world.item.StandingAndWallBlockItem;
#else
	import net.minecraft.world.item.SignItem;
#endif

public class ColoredSignItem
extends #if MC_VERSION >= MC_26_3_0 StandingAndWallBlockItem #else SignItem #endif
{

	public ColoredSignItem(Properties settings, Block standingBlock, Block wallBlock) {
		super(standingBlock, wallBlock, #if MC_VERSION >= MC_26_3_0 Direction.DOWN, #endif settings);
	}
}