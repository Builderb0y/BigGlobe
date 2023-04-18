package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record WoodPaletteEntry(RegistryEntry<WoodPalette> entry) {

	public static final TypeInfo TYPE = type(WoodPaletteEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static WoodPaletteEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static WoodPaletteEntry of(String id) {
		return new WoodPaletteEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, new Identifier(id)))
		);
	}

	public WoodPalette palette() {
		return this.entry.value();
	}

	public Block getBlock(WoodPaletteType type) {
		Block block = this.palette().blocks.get(type);
		if (block != null) return block;
		else throw new IllegalStateException("WoodPaletteType " + type + " not present on WoodPalette " + UnregisteredObjectException.getID(this.entry));
	}

	public BlockState getState(WoodPaletteType type) {
		return this.getBlock(type).getDefaultState();
	}

	public boolean isIn(WoodPaletteTagKey key) {
		return this.entry.isIn(key.key());
	}
}