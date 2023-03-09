package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList.Named;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public record StructureTypeTagKey(TagKey<StructureType<?>> key) implements TagWrapper<StructureTypeEntry> {

	public static final TypeInfo TYPE = TypeInfo.of(StructureTypeTagKey.class);

	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureTypeTagKey.class, "of", String.class, StructureTypeTagKey.class);

	public static StructureTypeTagKey of(MethodHandles.Lookup lookup, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureTypeTagKey of(String id) {
		return new StructureTypeTagKey(TagKey.of(Registry.STRUCTURE_TYPE_KEY, new Identifier(id)));
	}

	@Override
	public StructureTypeEntry random(RandomGenerator random) {
		Optional<Named<StructureType<?>>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_TYPE_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Structure tag does not exist: " + this.key.id());
		Optional<RegistryEntry<StructureType<?>>> feature = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (feature.isEmpty()) throw new RuntimeException("Structure tag is empty: " + this.key.id());
		return new StructureTypeEntry(feature.get());
	}

	@NotNull
	@Override
	public Iterator<StructureTypeEntry> iterator() {
		Optional<Named<StructureType<?>>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_TYPE_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Structure tag does not exist: " + this.key.id());
		return list.get().stream().map(StructureTypeEntry::new).iterator();
	}
}