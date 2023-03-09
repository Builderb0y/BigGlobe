package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public record StructureEntry(RegistryEntry<Structure> entry) {

	public static final TypeInfo TYPE = TypeInfo.of(StructureEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureEntry.class, "of", String.class, StructureEntry.class);

	public static StructureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureEntry of(String id) {
		return new StructureEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(Registry.STRUCTURE_KEY)
			.entryOf(RegistryKey.of(Registry.STRUCTURE_KEY, new Identifier(id)))
		);
	}

	public boolean isIn(StructureTagKey tag) {
		return this.entry.isIn(tag.key());
	}

	public StructureTypeEntry type() {
		return new StructureTypeEntry(
			Registry.STRUCTURE_TYPE.entryOf(
				Registry.STRUCTURE_TYPE.getKey(
					this.entry.value().getType()
				)
				.orElseThrow()
			)
		);
	}

	public String generationStep() {
		return this.entry.value().getFeatureGenerationStep().asString();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof StructureEntry that &&
			this.entry.getKey().orElseThrow().equals(that.entry.getKey().orElseThrow())
		);
	}

	@Override
	public int hashCode() {
		return this.entry.getKey().orElseThrow().hashCode();
	}

	@Override
	public String toString() {
		return "Structure: { " + this.entry.getKey().orElseThrow().getValue() + " }";
	}
}