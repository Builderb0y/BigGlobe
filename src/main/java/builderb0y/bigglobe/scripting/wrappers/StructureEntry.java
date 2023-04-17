package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructureEntry {

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
		return new StructureEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(RegistryKeys.STRUCTURE)
			.entryOf(RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(id)))
		);
	}

	public boolean isIn(StructureTagKey tag) {
		return this.entry.isIn(tag.key());
	}

	public StructureTypeEntry type() {
		if (this.type == null) {
			this.type = new StructureTypeEntry(
				Registries.STRUCTURE_TYPE.entryOf(
					UnregisteredObjectException.getKey(
						Registries.STRUCTURE_TYPE,
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