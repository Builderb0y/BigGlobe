package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record BiomeEntry(RegistryEntry<Biome> entry) implements EntryWrapper<Biome, BiomeTagKey> {

	public static final TypeInfo TYPE = type(BiomeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static BiomeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static BiomeEntry of(String id) {
		if (id == null) return null;
		return new BiomeEntry(
			BigGlobeMod
			.getRegistry(RegistryKeyVersions.biome())
			.getByName(id)
		);
	}

	@Override
	public boolean isIn(BiomeTagKey tag) {
		return this.isInImpl(tag);
	}

	public float temperature() {
		return this.entry.value().getTemperature();
	}

	public float downfall() {
		return ((BiomeDownfallAccessor)(Object)(this.entry.value())).bigglobe_getDownfall();
	}

	@Override
	public int hashCode() {
		return UnregisteredObjectException.getKey(this.entry).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof BiomeEntry that &&
			UnregisteredObjectException.getKey(this.entry) == UnregisteredObjectException.getKey(that.entry)
		);
	}

	@Override
	public String toString() {
		return "Biome: { " + UnregisteredObjectException.getID(this.entry) + " }";
	}

	/** implemented by {@link Biome}, but only needed in 1.19.3 and later. */
	public static interface BiomeDownfallAccessor {

		public abstract float bigglobe_getDownfall();
	}
}