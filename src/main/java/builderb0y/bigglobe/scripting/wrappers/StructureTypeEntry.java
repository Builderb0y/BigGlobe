package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructureTypeEntry(RegistryEntry<StructureType<?>> entry) {

	public static final TypeInfo TYPE = type(StructureTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureTypeEntry.class, "of", String.class, StructureTypeEntry.class);

	public static StructureTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureTypeEntry of(String id) {
		return new StructureTypeEntry(Registry.STRUCTURE_TYPE.entryOf(RegistryKey.of(Registry.STRUCTURE_TYPE_KEY, new Identifier(id))));
	}

	public boolean isIn(StructureTypeTagKey key) {
		return this.entry.isIn(key.key());
	}
}