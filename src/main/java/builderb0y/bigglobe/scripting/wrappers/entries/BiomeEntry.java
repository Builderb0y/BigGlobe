package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import builderb0y.bigglobe.mixinInterfaces.BiomeDownfallAccessor;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class BiomeEntry extends EntryWrapper<Biome, BiomeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(BiomeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public BiomeEntry(Holder<Biome> entry) {
		super(entry);
	}

	public static BiomeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static BiomeEntry of(String id, int flags) {
		Holder<Biome> entry = ConstantFactory.getEntry(Registries.BIOME, id, flags);
		return entry != null ? new BiomeEntry(entry) : null;
	}

	public float temperature() {
		return this.object().getBaseTemperature();
	}

	public float downfall() {
		return ((BiomeDownfallAccessor)(Object)(this.object())).bigglobe_getDownfall();
	}

	@Override
	public boolean isIn(BiomeTag entries) {
		return super.isIn(entries);
	}
}