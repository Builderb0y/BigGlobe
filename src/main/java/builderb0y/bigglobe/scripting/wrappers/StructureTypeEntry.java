package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructureTypeEntry(RegistryEntry<StructureType<?>> entry) implements EntryWrapper<StructureType<?>, StructureTypeTagKey> {

	public static final TypeInfo TYPE = type(StructureTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructureTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureTypeEntry of(String id) {
		if (id == null) return null;
		return new StructureTypeEntry(RegistryVersions.structureType().entryOf(RegistryKey.of(RegistryKeyVersions.structureType(), new Identifier(id))));
	}

	@Override
	public boolean isIn(StructureTypeTagKey key) {
		return this.isInImpl(key);
	}
}