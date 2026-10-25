package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.entries.StructurePlacementScriptEntry;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StructurePlacementScriptTag extends TagWrapper<CombinedStructureScripts, StructurePlacementScriptEntry> {

	public static final TypeInfo TYPE = type(StructurePlacementScriptTag.class);
	public static final TagParser PARSER = new TagParser("StructurePlacementScriptTag", StructurePlacementScriptTag.class, "StructurePlacementScript", MethodInfo.findMethod(StructurePlacementScriptEntry.class, "isIn", boolean.class, StructurePlacementScriptTag.class));

	public StructurePlacementScriptTag(DelayedEntryList<CombinedStructureScripts> list) {
		super(list);
	}

	public static StructurePlacementScriptTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static StructurePlacementScriptTag of(int flags, String... ids) {
		return new StructurePlacementScriptTag(DelayedEntryList.emptyOnClient(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public StructurePlacementScriptEntry wrap(Holder<CombinedStructureScripts> entry) {
		return new StructurePlacementScriptEntry(entry);
	}

	@Override
	public Holder<CombinedStructureScripts> unwrap(StructurePlacementScriptEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(StructurePlacementScriptEntry entry) {
		return super.contains(entry);
	}

	@Override
	public StructurePlacementScriptEntry random(RandomGenerator random) {
		return super.random(random);
	}

	@Override
	public StructurePlacementScriptEntry random(long seed) {
		return super.random(seed);
	}
}