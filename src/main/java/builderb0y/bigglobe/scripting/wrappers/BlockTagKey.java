package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList.Named;

import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record BlockTagKey(TagKey<Block> key) implements TagWrapper<Block> {

	public static final TypeInfo TYPE = type(BlockTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockTagKey.class, "of", String.class, BlockTagKey.class);

	public static BlockTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static BlockTagKey of(String id) {
		return new BlockTagKey(TagKey.of(Registry.BLOCK_KEY, new Identifier(id)));
	}

	@Override
	public Block random(RandomGenerator random) {
		Optional<Named<Block>> list = Registry.BLOCK.getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Block tag does not exist: " + this.key.id());
		Optional<RegistryEntry<Block>> block = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (block.isEmpty()) throw new RuntimeException("Block tag is empty: " + this.key.id());
		return block.get().value();
	}

	@Override
	public Iterator<Block> iterator() {
		Optional<Named<Block>> list = Registry.BLOCK.getEntryList(this.key);
		if (list.isEmpty()) throw new RuntimeException("Block tag does not exist: " + this.key.id());
		return list.get().stream().map(RegistryEntry::value).iterator();
	}
}