package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructureTypeTagKey(TagKey<StructureType<?>> key) implements TagWrapper<StructureType<?>, StructureTypeEntry> {

	public static final TypeInfo TYPE = type(StructureTypeTagKey.class);

	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructureTypeTagKey of(MethodHandles.Lookup lookup, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureTypeTagKey of(String id) {
		if (id == null) return null;
		return new StructureTypeTagKey(TagKey.of(RegistryKeyVersions.structureType(), new Identifier(id)));
	}

	@Override
	public StructureTypeEntry wrap(RegistryEntry<StructureType<?>> entry) {
		return new StructureTypeEntry(entry);
	}

	@Override
	public StructureTypeEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public StructureTypeEntry random(long seed) {
		return this.randomImpl(seed);
	}
}