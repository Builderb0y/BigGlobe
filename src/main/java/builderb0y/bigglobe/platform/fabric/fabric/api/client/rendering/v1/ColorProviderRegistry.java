package builderb0y.bigglobe.platform.fabric.fabric.api.client.rendering.v1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public interface ColorProviderRegistry<T, P> {

	ColorProviderRegistry<Block, BlockColor> BLOCK = new BlockImpl();
	ColorProviderRegistry<ItemLike, ItemColor> ITEM = new ItemImpl();

	void register(P provider, T... objects);

	class BlockImpl implements ColorProviderRegistry<Block, BlockColor> {
		public static class Entry {
			public final BlockColor provider;
			public final Block[] blocks;
			public Entry(BlockColor provider, Block[] blocks) {
				this.provider = provider;
				this.blocks = blocks;
			}
		}

		public static final List<Entry> ENTRIES = new ArrayList<>();

		@Override
		public void register(BlockColor provider, Block... objects) {
			ENTRIES.add(new Entry(provider, objects));
		}
	}

	class ItemImpl implements ColorProviderRegistry<ItemLike, ItemColor> {
		public static class Entry {
			public final ItemColor provider;
			public final ItemLike[] items;
			public Entry(ItemColor provider, ItemLike[] items) {
				this.provider = provider;
				this.items = items;
			}
		}

		public static final List<Entry> ENTRIES = new ArrayList<>();

		@Override
		public void register(ItemColor provider, ItemLike... objects) {
			ENTRIES.add(new Entry(provider, objects));
		}
	}
}
