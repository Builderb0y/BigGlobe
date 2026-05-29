package builderb0y.bigglobe.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.autocodec.annotations.UseImplementation;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockOrTagProperties;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;

@Wrapper
public class BlockState2ObjectMap<V> implements DelayedCompileable {

	public final @UseImplementation(LinkedHashMap.class) Map<String, V> serializedStates;
	public final transient Map<BlockState, V> runtimeStates;

	public BlockState2ObjectMap() {
		this.serializedStates = new HashMap<>();
		this.runtimeStates = new HashMap<>();
	}

	public BlockState2ObjectMap(Map<String, V> serializedStates) {
		this.serializedStates = serializedStates;
		this.runtimeStates = new HashMap<>(serializedStates.size());
	}

	@Override
	public void compile(ColumnEntryRegistry registry) {
		BetterRegistry<Block> blockRegistry = registry.registries.getRegistry(Registries.BLOCK);
		for (Map.Entry<String, V> serializedEntry : this.serializedStates.entrySet()) {
			BlockOrTagProperties properties = (
				BlockStateCoder
					.decodeBlockOrTag(blockRegistry, serializedEntry.getKey())
					.unwrapNullableEager(BigGlobeMod.LOGGER::warn)
			);
			if (properties != null) {
				properties
					.getMatchingStates()
					.sequential()
					.forEach((BlockState state) -> this.runtimeStates.put(state, serializedEntry.getValue()));
			}
		}
	}
}