package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import builderb0y.bigglobe.scripting.wrappers.BlockWrapper;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockTag extends TagWrapper<Block, Block> {

	public static final TypeInfo TYPE = type(BlockTag.class);
	public static final TagParser PARSER = new TagParser("BlockTag", BlockTag.class, "Block", MethodInfo.findMethod(BlockWrapper.class, "isIn", boolean.class, Block.class, BlockTag.class));

	public BlockTag(DelayedEntryList<Block> list) {
		super(list);
	}

	public static BlockTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static BlockTag of(int flags, String... ids) {
		return new BlockTag(DelayedEntryList.create(Registries.BLOCK, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public Block wrap(Holder<Block> entry) {
		return entry.value();
	}

	@Override
	@SuppressWarnings("deprecation")
	public Holder<Block> unwrap(Block block) {
		return block.builtInRegistryHolder();
	}

	@Override
	public boolean contains(Block block) {
		return super.contains(block);
	}

	@Override
	public Block random(RandomGenerator random) {
		return super.random(random);
	}

	@Override
	public Block random(long seed) {
		return super.random(seed);
	}
}