package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.random.RandomGenerator;

import com.google.common.collect.Maps;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.MappingRandomList;
import builderb0y.bigglobe.scripting.wrappers.tags.WoodPaletteTag;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public class WoodPaletteEntry extends EntryWrapper<WoodPalette, WoodPaletteTag> {

	public static final Info INFO = new Info();
	public static final class Info extends InfoHolder {

		public MethodInfo
			features,
			getBlocks,
			getRandomBlock,
			getRandomState,
			getSeededBlock,
			getSeededState;
	}
	public static final WoodPalette CLIENT_EMPTY = new WoodPalette(new EnumMap<>(WoodPaletteType.class), Collections.emptyMap());
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public WoodPaletteEntry(RegistryEntry<WoodPalette> entry) {
		super(entry);
	}

	public static WoodPaletteEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static WoodPaletteEntry of(String id, int flags) {
		RegistryEntry<WoodPalette> entry = ConstantFactory.getEntryServerOnly(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, id, flags, CLIENT_EMPTY);
		return entry != null ? new WoodPaletteEntry(entry) : null;
	}

	public Map<String, ConfiguredFeatureEntry> features() {
		return Collections.unmodifiableMap(Maps.transformValues(this.entry.value().features, ConfiguredFeatureEntry::new));
	}

	public IRandomList<Block> getBlocks(WoodPaletteType type) {
		IRandomList<RegistryEntry<Block>> blocks = this.entry.value().blocks.get(type);
		if (blocks != null) return MappingRandomList.create(blocks, RegistryEntry<Block>::value);
		else throw new IllegalStateException("WoodPaletteType " + type + " not present on WoodPalette " + UnregisteredObjectException.getID(this.entry));
	}

	public Block getRandomBlock(RandomGenerator random, WoodPaletteType type) {
		return this.getBlocks(type).getRandomElement(random);
	}

	public BlockState getRandomState(RandomGenerator random, WoodPaletteType type) {
		return this.getRandomBlock(random, type).getDefaultState();
	}

	public Block getSeededBlock(long seed, WoodPaletteType type) {
		return this.getBlocks(type).getRandomElement(seed);
	}

	public BlockState getSeededState(long seed, WoodPaletteType type) {
		return this.getSeededBlock(seed, type).getDefaultState();
	}

	@Override
	public boolean isIn(WoodPaletteTag entries) {
		return super.isIn(entries);
	}
}