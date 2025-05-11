package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.mixinInterfaces.BiomeDownfallAccessor;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class BiomeEntry extends EntryWrapper<Biome, BiomeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(BiomeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public BiomeEntry(RegistryEntry<Biome> entry) {
		super(entry);
	}

	public static BiomeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static BiomeEntry of(String id, int flags) {
		RegistryEntry<Biome> entry = ConstantFactory.getEntry(RegistryKeys.BIOME, id, flags);
		return entry != null ? new BiomeEntry(entry) : null;
	}

	public float temperature() {
		return this.object().getTemperature();
	}

	public float downfall() {
		return ((BiomeDownfallAccessor)(Object)(this.object())).bigglobe_getDownfall();
	}

	@Override
	public boolean isIn(BiomeTag entries) {
		return super.isIn(entries);
	}
}