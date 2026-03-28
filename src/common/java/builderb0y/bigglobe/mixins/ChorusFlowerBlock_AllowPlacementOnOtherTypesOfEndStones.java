package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerBlock_AllowPlacementOnOtherTypesOfEndStones {

	@Redirect(method = { "randomTick", "canSurvive" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private boolean bigglobe_allowPlacementOnOtherTypesOfEndStones1(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.is(BigGlobeBlockTags.END_STONES) : state.is(block);
	}
}