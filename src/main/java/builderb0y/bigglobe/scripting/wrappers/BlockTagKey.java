package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.block.Block;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record BlockTagKey(TagKey<Block> key) implements TagWrapper<Block, Block> {

	public static final TypeInfo TYPE = type(BlockTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static BlockTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static BlockTagKey of(String id) {
		if (id == null) return null;
		return new BlockTagKey(TagKey.of(RegistryKeyVersions.block(), new Identifier(id)));
	}

	@Override
	public Block wrap(RegistryEntry<Block> entry) {
		return entry.value();
	}

	@Override
	public Block random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public Block random(long seed) {
		return this.randomImpl(seed);
	}
}