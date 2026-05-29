package builderb0y.bigglobe.mixins;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import builderb0y.bigglobe.mixinInterfaces.MutableBlockEntityType;

@Mixin(BlockEntityType.class)
public class BlockEntityType_AddBlockHook implements MutableBlockEntityType {

	@Shadow
	@Final
	@Mutable
	private Set<Block> validBlocks;

	@Override
	public Set<Block> bigglobe_getBlocks() {
		return this.validBlocks;
	}

	@Override
	public void bigglobe_addValidBlock(Block block) {
		try {
			this.validBlocks.add(block);
		}
		catch (RuntimeException ignored) {
			this.validBlocks = new HashSet<>(this.validBlocks);
			this.validBlocks.add(block);
		}
	}
}