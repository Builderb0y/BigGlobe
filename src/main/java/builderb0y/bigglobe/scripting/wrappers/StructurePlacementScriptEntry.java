package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.bigglobe.structures.scripted.StructurePlacementScript;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructurePlacementScriptEntry(RegistryEntry<StructurePlacementScript.Holder> entry) {

	public static final TypeInfo TYPE = type(StructurePlacementScriptEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructurePlacementScriptEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructurePlacementScriptEntry of(String id) {
		return new StructurePlacementScriptEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_KEY, new Identifier(id)))
		);
	}

	public boolean isIn(StructurePlacementScriptTagKey key) {
		return this.entry.isIn(key.key());
	}
}