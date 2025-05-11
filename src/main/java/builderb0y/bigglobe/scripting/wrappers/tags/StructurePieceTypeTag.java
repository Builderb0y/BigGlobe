package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;

import builderb0y.bigglobe.scripting.wrappers.entries.StructurePieceTypeEntry;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StructurePieceTypeTag extends TagWrapper<StructurePieceType, StructurePieceTypeEntry> {

	public static final TypeInfo TYPE = type(StructurePieceTypeTag.class);
	public static final TagParser PARSER = new TagParser("StructurePieceTypeTag", StructurePieceTypeTag.class, "StructurePieceType", MethodInfo.findMethod(StructurePieceTypeEntry.class, "isIn", boolean.class, StructurePieceTypeTag.class));

	public StructurePieceTypeTag(DelayedEntryList<StructurePieceType> list) {
		super(list);
	}

	public static StructurePieceTypeTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static StructurePieceTypeTag of(int flags, String... ids) {
		return new StructurePieceTypeTag(DelayedEntryList.create(RegistryKeys.STRUCTURE_PIECE, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public StructurePieceTypeEntry wrap(RegistryEntry<StructurePieceType> entry) {
		return new StructurePieceTypeEntry(entry);
	}

	@Override
	public RegistryEntry<StructurePieceType> unwrap(StructurePieceTypeEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(StructurePieceTypeEntry entry) {
		return super.contains(entry);
	}

	@Override
	public StructurePieceTypeEntry random(RandomGenerator random) {
		return super.random(random);
	}

	@Override
	public StructurePieceTypeEntry random(long seed) {
		return super.random(seed);
	}
}