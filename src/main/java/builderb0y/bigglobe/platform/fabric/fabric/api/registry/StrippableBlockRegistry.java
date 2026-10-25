package builderb0y.bigglobe.platform.fabric.fabric.api.registry;

import java.util.Map;
import com.google.common.collect.Maps;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;

public class StrippableBlockRegistry {

	public static void register(Block input, Block stripped) {
		try {
			if (!(AxeItem.STRIPPABLES instanceof Map)) {
				AxeItem.STRIPPABLES = Maps.newHashMap(AxeItem.STRIPPABLES);
			}
			AxeItem.STRIPPABLES.put(input, stripped);
		}
		catch (Throwable ignored) {}
	}
}
