package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructurePieceTypeEntry(RegistryEntry<StructurePieceType> entry) implements EntryWrapper<StructurePieceType, StructurePieceTypeTagKey> {

	public static final TypeInfo TYPE = type(StructurePieceType.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructurePieceTypeEntry.class, "of", String.class, StructurePieceType.class);

	public static StructurePieceType of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructurePieceType of(String id) {
		if (id == null) return null;
		StructurePieceType type = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeyVersions.structurePieceType()).get(new Identifier(id));
		if (type != null) return type;
		else throw new IllegalArgumentException("Unknown structure piece type: " + id);
	}

	@Override
	public boolean isIn(StructurePieceTypeTagKey key) {
		return this.isInImpl(key);
	}
}