package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import builderb0y.bigglobe.scripting.wrappers.tags.StructureTypeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructureTypeEntry extends EntryWrapper<StructureType<?>, StructureTypeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(StructureTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public StructureTypeEntry(Holder<StructureType<?>> entry) {
		super(entry);
	}

	public static StructureTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static StructureTypeEntry of(String id, int flags) {
		Holder<StructureType<?>> entry = ConstantFactory.getEntry(Registries.STRUCTURE_TYPE, id, flags);
		return entry != null ? new StructureTypeEntry(entry) : null;
	}

	@Override
	public boolean isIn(StructureTypeTag entries) {
		return super.isIn(entries);
	}
}