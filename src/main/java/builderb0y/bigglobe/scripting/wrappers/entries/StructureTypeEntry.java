package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.scripting.wrappers.tags.StructureTypeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructureTypeEntry extends EntryWrapper<StructureType<?>, StructureTypeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(StructureTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public StructureTypeEntry(RegistryEntry<StructureType<?>> entry) {
		super(entry);
	}

	public static StructureTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static StructureTypeEntry of(String id, int flags) {
		RegistryEntry<StructureType<?>> entry = ConstantFactory.getEntry(RegistryKeys.STRUCTURE_TYPE, id, flags);
		return entry != null ? new StructureTypeEntry(entry) : null;
	}

	@Override
	public boolean isIn(StructureTypeTag entries) {
		return super.isIn(entries);
	}
}