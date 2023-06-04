package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructureTypeTagKey(TagKey<StructureType<?>> key) implements TagWrapper<StructureType<?>, StructureTypeEntry> {

	public static final TypeInfo TYPE = type(StructureTypeTagKey.class);

	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructureTypeTagKey of(MethodHandles.Lookup lookup, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructureTypeTagKey of(String id) {
		return new StructureTypeTagKey(TagKey.of(Registry.STRUCTURE_TYPE_KEY, new Identifier(id)));
	}

	@Override
	public StructureTypeEntry wrap(RegistryEntry<StructureType<?>> entry) {
		return new StructureTypeEntry(entry);
	}

	@Override
	public StructureTypeEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}
}