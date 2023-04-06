package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
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
		return new BlockTagKey(TagKey.of(RegistryKeys.BLOCK, new Identifier(id)));
	}

	@Override
	public Block random(RandomGenerator random) {
		RegistryEntryList<Block> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.BLOCK).getEntryList(this.key).orElse(null);
		if (list == null) throw new RuntimeException("Block tag does not exist: " + this.key.id());
		Optional<RegistryEntry<Block>> block = list.getRandom(new MojangPermuter(random.nextLong()));
		if (block.isEmpty()) throw new RuntimeException("Block tag is empty: " + this.key.id());
		return block.get().value();
	}

	@Override
	public Iterator<Block> iterator() {
		RegistryEntryList<Block> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.BLOCK).getEntryList(this.key).orElse(null);
		if (list == null) throw new RuntimeException("Block tag does not exist: " + this.key.id());
		return list.stream().map(RegistryEntry::value).iterator();
	}
}