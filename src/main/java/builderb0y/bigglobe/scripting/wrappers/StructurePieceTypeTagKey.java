package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.structure.StructurePieceType;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;

import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record StructurePieceTypeTagKey(TagKey<StructurePieceType> key) implements TagWrapper<StructurePieceType, StructurePieceTypeEntry> {

	public static final TypeInfo TYPE = type(StructurePieceTypeTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static StructurePieceTypeTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructurePieceTypeTagKey of(String id) {
		return new StructurePieceTypeTagKey(TagKey.of(Registry.STRUCTURE_PIECE_KEY, new Identifier(id)));
	}

	@Override
	public StructurePieceTypeEntry wrap(RegistryEntry<StructurePieceType> entry) {
		return new StructurePieceTypeEntry(entry);
	}

	@Override
	public StructurePieceTypeEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}
}