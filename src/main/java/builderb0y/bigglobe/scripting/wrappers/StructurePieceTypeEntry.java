package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructurePieceTypeEntry(RegistryEntry<StructurePieceType> entry) implements EntryWrapper<StructurePieceType, StructurePieceTypeTagKey> {

	public static final TypeInfo TYPE = type(StructurePieceTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructurePieceTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructurePieceTypeEntry of(String id) {
		if (id == null) return null;
		RegistryEntry<StructurePieceType> type = BigGlobeMod.getRegistry(RegistryKeyVersions.structurePieceType()).getByName(id);
		if (type != null) return new StructurePieceTypeEntry(type);
		else throw new IllegalArgumentException("Unknown structure piece type: " + id);
	}

	@Override
	public boolean isIn(StructurePieceTypeTagKey key) {
		return this.isInImpl(key);
	}
}