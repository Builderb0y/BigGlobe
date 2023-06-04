package builderb0y.bigglobe.mixinInterfaces;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;

import builderb0y.bigglobe.mixins.BlockEntityType_AddBlockHook;

/** implemented by {@link BlockEntityType} by {@link BlockEntityType_AddBlockHook} */
public interface MutableBlockEntityType {

	public abstract Set<Block> bigglobe_getBlocks();

	public abstract void bigglobe_addValidBlock(Block block);
}