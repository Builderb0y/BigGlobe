package builderb0y.bigglobe.platform.fabric.fabric.api.registry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TillableBlockRegistry {

	@SuppressWarnings("unchecked")
	private static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = getTillables();

	@SuppressWarnings("unchecked")
	private static Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> getTillables() {
		try {
			Field field = HoeItem.class.getDeclaredField("TILLABLES");
			field.setAccessible(true);
			return (Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>>) field.get(null);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to access HoeItem.TILLABLES", e);
		}
	}

	public static void register(Block input, Predicate<UseOnContext> condition, BlockState output) {
		TILLABLES.put(input, Pair.of(condition, HoeItem.changeIntoState(output)));
	}

	public static void register(Block input, Predicate<UseOnContext> condition, Consumer<UseOnContext> action) {
		TILLABLES.put(input, Pair.of(condition, action));
	}
}
