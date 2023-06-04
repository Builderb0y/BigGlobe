package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryEntry;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record WoodPaletteTagKey(TagKey<WoodPalette> key) implements TagWrapper<WoodPalette, WoodPaletteEntry> {

	public static final TypeInfo TYPE = type(WoodPaletteTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static WoodPaletteTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static WoodPaletteTagKey of(String id) {
		return new WoodPaletteTagKey(TagKey.of(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, new Identifier(id)));
	}

	@Override
	public WoodPaletteEntry wrap(RegistryEntry<WoodPalette> entry) {
		return new WoodPaletteEntry(entry);
	}

	@Override
	public WoodPaletteEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}
}