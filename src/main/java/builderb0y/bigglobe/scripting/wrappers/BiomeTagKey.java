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
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record BiomeTagKey(TagKey<Biome> key) implements TagWrapper<BiomeEntry> {

	public static final TypeInfo TYPE = type(BiomeTagKey.class);
	public static final MethodInfo
		RANDOM = method(ACC_PUBLIC, BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class),
		ITERATOR = method(ACC_PUBLIC, BiomeTagKey.class, "iterator", Iterator.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BiomeTagKey.class, "of", String.class, BiomeTagKey.class);

	public static BiomeTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static BiomeTagKey of(String name) {
		return new BiomeTagKey(TagKey.of(Registry.BIOME_KEY, new Identifier(name)));
	}

	@Override
	public BiomeEntry random(RandomGenerator random) {
		Optional<Named<Biome>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.BIOME_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
		Optional<RegistryEntry<Biome>> biome = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (biome.isEmpty()) throw new RuntimeException("Biome tag is empty: " + this.key.id());
		return new BiomeEntry(biome.get());
	}

	@Override
	public Iterator<BiomeEntry> iterator() {
		Optional<Named<Biome>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.BIOME_KEY).getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
		return list.get().stream().map(BiomeEntry::new).iterator();
	}
}