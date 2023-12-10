package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusPlantBlock;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;

@Mixin(ChorusPlantBlock.class)
public class ChorusPlantBlock_AllowPlacementOnOtherTypesOfEndStones {

	@Redirect(method = { "getStateForNeighborUpdate", "canPlaceAt" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
	private boolean bigglobe_allowPlacementOnOtherTypesOfEndStones1(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.isIn(BigGlobeBlockTags.END_STONES) : state.isOf(block);
	}

	@Redirect(method = "withConnectionProperties", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
	private #if MC_VERSION >= MC_1_20_3 static #endif boolean bigglobe_allowPlacementOnOtherTypesOfEndStones2(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.isIn(BigGlobeBlockTags.END_STONES) : state.isOf(block);
	}
}