package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ChorusPlantBlock.class)
public class ChorusPlantBlock_AllowPlacementOnOtherTypesOfEndStones {

	@Redirect(method = { "updateShape", "canSurvive" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private boolean bigglobe_allowPlacementOnOtherTypesOfEndStones1(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.is(BigGlobeBlockTags.END_STONES) : state.is(block);
	}

	@Redirect(method = "getStateWithConnections", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private static boolean bigglobe_allowPlacementOnOtherTypesOfEndStones2(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.is(BigGlobeBlockTags.END_STONES) : state.is(block);
	}
}