package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.tags.StructurePlacementScriptTag;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructurePlacementScriptEntry extends EntryWrapper<CombinedStructureScripts, StructurePlacementScriptTag> {

	public static final TypeInfo TYPE = TypeInfo.of(StructurePlacementScriptEntry.class);
	public static final CombinedStructureScripts CLIENT_SCRIPTS = new CombinedStructureScripts(null, null);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public StructurePlacementScriptEntry(RegistryEntry<CombinedStructureScripts> entry) {
		super(entry);
	}

	public static StructurePlacementScriptEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static StructurePlacementScriptEntry of(String id, int flags) {
		RegistryEntry<CombinedStructureScripts> entry = ConstantFactory.getEntryServerOnly(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY, id, flags, CLIENT_SCRIPTS);
		return entry != null ? new StructurePlacementScriptEntry(entry) : null;
	}

	@Override
	public boolean isIn(StructurePlacementScriptTag entries) {
		return super.isIn(entries);
	}
}