package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;

import builderb0y.bigglobe.scripting.wrappers.tags.StructurePieceTypeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructurePieceTypeEntry extends EntryWrapper<StructurePieceType, StructurePieceTypeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(StructurePieceTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public StructurePieceTypeEntry(RegistryEntry<StructurePieceType> entry) {
		super(entry);
	}

	public static StructurePieceTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static StructurePieceTypeEntry of(String id, int flags) {
		RegistryEntry<StructurePieceType> entry = ConstantFactory.getEntry(RegistryKeys.STRUCTURE_PIECE, id, flags);
		return entry != null ? new StructurePieceTypeEntry(entry) : null;
	}

	@Override
	public boolean isIn(StructurePieceTypeTag entries) {
		return super.isIn(entries);
	}
}