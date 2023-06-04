package builderb0y.bigglobe.mixins;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;

import builderb0y.bigglobe.mixinInterfaces.MutableBlockEntityType;

@Mixin(BlockEntityType.class)
public class BlockEntityType_AddBlockHook implements MutableBlockEntityType {

	@Shadow @Final @Mutable private Set<Block> blocks;

	@Override
	public Set<Block> bigglobe_getBlocks() {
		return this.blocks;
	}

	@Override
	public void bigglobe_addValidBlock(Block block) {
		try {
			this.blocks.add(block);
		}
		catch (RuntimeException ignored) {
			this.blocks = new HashSet<>(this.blocks);
			this.blocks.add(block);
		}
	}
}