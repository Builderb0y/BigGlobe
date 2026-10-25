package net.fabricmc.fabric.api.registry;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;

public class FlammableBlockRegistry {

	public static class Entry {
		public final int burnChance;
		public final int spreadChance;
		public Entry(int burnChance, int spreadChance) {
			this.burnChance = burnChance;
			this.spreadChance = spreadChance;
		}
	}

	private static final FlammableBlockRegistry DEFAULT = new FlammableBlockRegistry();
	private final Map<Block, Entry> entries = new HashMap<>();

	public static FlammableBlockRegistry getDefaultInstance() {
		return DEFAULT;
	}

	public static FlammableBlockRegistry getInstance(Block block) {
		return DEFAULT;
	}

	public void add(Block block, int burnChance, int spreadChance) {
		this.entries.put(block, new Entry(burnChance, spreadChance));
		if (Blocks.FIRE instanceof FireBlock fireBlock) {
			fireBlock.setFlammable(block, burnChance, spreadChance);
		}
	}

	public Map<Block, Entry> getEntries() {
		return this.entries;
	}
}
