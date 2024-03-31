package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructureEntry implements EntryWrapper<Structure, StructureTagKey> {

	public static final TypeInfo TYPE = TypeInfo.of(StructureEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public final RegistryEntry<Structure> entry;
	public StructureTypeEntry type;

	public StructureEntry(RegistryEntry<Structure> entry) {
		this.entry = entry;
	}

	public static StructureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureEntry of(String id) {
		if (id == null) return null;
		return new StructureEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(RegistryKeyVersions.structure())
			.entryOf(RegistryKey.of(RegistryKeyVersions.structure(), new Identifier(id)))
		);
	}

	@Override
	public RegistryEntry<Structure> entry() {
		return this.entry;
	}

	@Override
	public boolean isIn(StructureTagKey tag) {
		return this.isInImpl(tag);
	}

	public StructureTypeEntry type() {
		if (this.type == null) {
			this.type = new StructureTypeEntry(
				RegistryVersions.structureType().entryOf(
					UnregisteredObjectException.getKey(
						RegistryVersions.structureType(),
						this.entry.value().getType()
					)
				)
			);
		}
		return this.type;
	}

	public String generationStep() {
		return this.entry.value().getFeatureGenerationStep().asString();
	}

	public BiomeTagKey validBiomes() {
		return new BiomeTagKey(this.entry.value().getValidBiomes().getTagKey().orElseThrow());
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof StructureEntry that &&
			UnregisteredObjectException.getKey(this.entry).equals(UnregisteredObjectException.getKey(that.entry))
		);
	}

	@Override
	public int hashCode() {
		return UnregisteredObjectException.getKey(this.entry).hashCode();
	}

	@Override
	public String toString() {
		return "Structure: { " + UnregisteredObjectException.getID(this.entry) + " }";
	}
}