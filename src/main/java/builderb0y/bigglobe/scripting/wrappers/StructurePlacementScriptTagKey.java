package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList.Named;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.bigglobe.structures.scripted.StructurePlacementScript;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructurePlacementScriptTagKey(TagKey<StructurePlacementScript.Holder> key) implements TagWrapper<StructurePlacementScriptEntry> {

	public static final TypeInfo TYPE = type(StructurePlacementScriptTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructurePlacementScriptTagKey of(MethodHandles.Lookup lookup, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructurePlacementScriptTagKey of(String id) {
		return new StructurePlacementScriptTagKey(TagKey.of(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_KEY, new Identifier(id)));
	}

	@Override
	public StructurePlacementScriptEntry random(RandomGenerator random) {
		Optional<Named<StructurePlacementScript.Holder>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Structure placement script tag does not exist: " + this.key.id());
		Optional<RegistryEntry<StructurePlacementScript.Holder>> script = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (script.isEmpty()) throw new RuntimeException("Structure placement script tag is empty: " + this.key.id());
		return new StructurePlacementScriptEntry(script.get());
	}

	@NotNull
	@Override
	public Iterator<StructurePlacementScriptEntry> iterator() {
		Optional<Named<StructurePlacementScript.Holder>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Structure placement script tag does not exist: " + this.key.id());
		return list.get().stream().map(StructurePlacementScriptEntry::new).iterator();
	}
}