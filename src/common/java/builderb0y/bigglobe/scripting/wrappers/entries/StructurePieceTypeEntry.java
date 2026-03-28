package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import builderb0y.bigglobe.scripting.wrappers.tags.StructurePieceTypeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructurePieceTypeEntry extends EntryWrapper<StructurePieceType, StructurePieceTypeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(StructurePieceTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public StructurePieceTypeEntry(Holder<StructurePieceType> entry) {
		super(entry);
	}

	public static StructurePieceTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static StructurePieceTypeEntry of(String id, int flags) {
		Holder<StructurePieceType> entry = ConstantFactory.getEntry(Registries.STRUCTURE_PIECE, id, flags);
		return entry != null ? new StructurePieceTypeEntry(entry) : null;
	}

	@Override
	public boolean isIn(StructurePieceTypeTag entries) {
		return super.isIn(entries);
	}
}