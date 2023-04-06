package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record BiomeEntry(RegistryEntry<Biome> biome) {

	public static final TypeInfo TYPE = type(BiomeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BiomeEntry.class, "of", String.class, BiomeEntry.class);

	public static BiomeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static BiomeEntry of(String id) {
		return new BiomeEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(RegistryKeys.BIOME)
			.entryOf(RegistryKey.of(RegistryKeys.BIOME, new Identifier(id)))
		);
	}

	public boolean isIn(BiomeTagKey key) {
		return this.biome.isIn(key.key());
	}

	public float temperature() {
		return this.biome.value().getTemperature();
	}

	public float downfall() {
		return ((BiomeDownfallAccessor)(Object)(this.biome.value())).bigglobe_getDownfall();
	}

	@Override
	public int hashCode() {
		return UnregisteredObjectException.getKey(this.biome).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof BiomeEntry that &&
			UnregisteredObjectException.getKey(this.biome).equals(UnregisteredObjectException.getKey(that.biome))
		);
	}

	@Override
	public String toString() {
		return "Biome: { " + UnregisteredObjectException.getID(this.biome) + " }";
	}

	/** implemented by {@link Biome}. */
	public static interface BiomeDownfallAccessor {

		public abstract float bigglobe_getDownfall();
	}
}