package builderb0y.bigglobe.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;

import builderb0y.autocodec.annotations.UseImplementation;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockProperties;
import builderb0y.bigglobe.codecs.BlockStateCoder.TagProperties;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
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
		BetterRegistry<Block> blockRegistry = registry.registries.getRegistry(RegistryKeys.BLOCK);
		for (Map.Entry<String, V> serializedEntry : this.serializedStates.entrySet()) {
			BlockStateCoder.decodeBlockOrTag(blockRegistry, serializedEntry.getKey()).fold(
				BlockProperties::allStates,
				TagProperties::collectStates
			)
			.sequential()
			.forEach((BlockState state) -> this.runtimeStates.put(state, serializedEntry.getValue()));
		}
	}
}