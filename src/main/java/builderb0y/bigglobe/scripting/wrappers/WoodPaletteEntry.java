package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

import com.google.common.collect.Collections2;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record WoodPaletteEntry(RegistryEntry<WoodPalette> entry) implements EntryWrapper<WoodPalette, WoodPaletteTagKey> {

	public static final TypeInfo TYPE = type(WoodPaletteEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static WoodPaletteEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static WoodPaletteEntry of(String id) {
		if (id == null) return null;
		return new WoodPaletteEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, new Identifier(id)))
		);
	}

	public static @Nullable WoodPaletteEntry randomForBiome(BiomeEntry biome, RandomGenerator random) {
		List<RegistryEntry<WoodPalette>> cache = WoodPalette.BIOME_CACHE.get().getOrDefault(biome.key(), Collections.emptyList());
		return switch (cache.size()) {
			case 0 -> null;
			case 1 -> new WoodPaletteEntry(cache.get(0));
			default -> new WoodPaletteEntry(cache.get(random.nextInt(cache.size())));
		};
	}

	public static Collection<WoodPaletteEntry> allForBiome(BiomeEntry biome) {
		List<RegistryEntry<WoodPalette>> cache = WoodPalette.BIOME_CACHE.get().get(biome.key());
		return cache == null ? Collections.emptyList() : Collections2.transform(cache, WoodPaletteEntry::new);
	}

	public WoodPalette palette() {
		return this.entry.value();
	}

	public IRandomList<Block> getBlocks(WoodPaletteType type) {
		IRandomList<Block> block = this.palette().blocks.get(type);
		if (block != null) return block;
		else throw new IllegalStateException("WoodPaletteType " + type + " not present on WoodPalette " + UnregisteredObjectException.getID(this.entry));
	}

	public Block getBlock(RandomGenerator random, WoodPaletteType type) {
		return this.getBlocks(type).getRandomElement(random);
	}

	public BlockState getState(RandomGenerator random, WoodPaletteType type) {
		return this.getBlock(random, type).getDefaultState();
	}

	@Override
	public boolean isIn(WoodPaletteTagKey key) {
		return this.isInImpl(key);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof WoodPaletteEntry that &&
			UnregisteredObjectException.getKey(this.entry) == UnregisteredObjectException.getKey(that.entry)
		);
	}

	@Override
	public int hashCode() {
		return UnregisteredObjectException.getKey(this.entry).hashCode();
	}

	@Override
	public String toString() {
		return "WoodPalette: { " + UnregisteredObjectException.getID(this.entry) + " }";
	}
}