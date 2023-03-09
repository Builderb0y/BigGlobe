package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList.Named;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public record StructureTagKey(TagKey<Structure> key) implements TagWrapper<StructureEntry> {

	public static final TypeInfo TYPE = TypeInfo.of(StructureTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureTagKey.class, "of", String.class, StructureTagKey.class);

	public static StructureTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureTagKey of(String id) {
		return new StructureTagKey(TagKey.of(Registry.STRUCTURE_KEY, new Identifier(id)));
	}

	@Override
	public StructureEntry random(RandomGenerator random) {
		Optional<Named<Structure>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Structure tag does not exist: " + this.key.id());
		Optional<RegistryEntry<Structure>> feature = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (feature.isEmpty()) throw new RuntimeException("Structure tag is empty: " + this.key.id());
		return new StructureEntry(feature.get());
	}

	@Override
	public Iterator<StructureEntry> iterator() {
		Optional<Named<Structure>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Structure tag does not exist: " + this.key.id());
		return list.get().stream().map(StructureEntry::new).iterator();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof StructureTagKey that &&
			this.key.id().equals(that.key.id())
		);
	}

	@Override
	public int hashCode() {
		return this.key.id().hashCode();
	}

	@Override
	public String toString() {
		return "StructureTag: { " + this.key.id() + " }";
	}
}