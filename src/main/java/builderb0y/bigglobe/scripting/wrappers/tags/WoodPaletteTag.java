package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.scripting.wrappers.entries.WoodPaletteEntry;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WoodPaletteTag extends TagWrapper<WoodPalette, WoodPaletteEntry> {

	public static final TypeInfo TYPE = type(WoodPaletteTag.class);
	public static final TagParser PARSER = new TagParser("WoodPaletteTag", WoodPaletteTag.class, "WoodPalette", MethodInfo.findMethod(WoodPaletteEntry.class, "isIn", boolean.class, WoodPaletteTag.class));

	public WoodPaletteTag(DelayedEntryList<WoodPalette> list) {
		super(list);
	}

	public static WoodPaletteTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static WoodPaletteTag of(int flags, String... ids) {
		return new WoodPaletteTag(DelayedEntryList.emptyOnClient(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public WoodPaletteEntry wrap(Holder<WoodPalette> entry) {
		return new WoodPaletteEntry(entry);
	}

	@Override
	public Holder<WoodPalette> unwrap(WoodPaletteEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(WoodPaletteEntry entry) {
		return super.contains(entry);
	}

	@Override
	public WoodPaletteEntry random(RandomGenerator random) {
		return super.random(random);
	}

	@Override
	public WoodPaletteEntry random(long seed) {
		return super.random(seed);
	}
}