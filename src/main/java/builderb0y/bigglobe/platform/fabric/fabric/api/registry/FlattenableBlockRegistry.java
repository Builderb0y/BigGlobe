package builderb0y.bigglobe.platform.fabric.fabric.api.registry;

import java.util.Map;
import com.google.common.collect.Maps;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FlattenableBlockRegistry {

	public static void register(Block input, BlockState flattened) {
		try {
			if (!(ShovelItem.FLATTENABLES instanceof Map)) {
				ShovelItem.FLATTENABLES = Maps.newHashMap(ShovelItem.FLATTENABLES);
			}
			ShovelItem.FLATTENABLES.put(input, flattened);
		}
		catch (Throwable ignored) {}
	}
}
