package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.feature.ChorusPlantFeature;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;

@Mixin(ChorusPlantFeature.class)
public class ChorusPlantFeature_AllowPlacementOnOtherTypesOfEndStones {

	@Redirect(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
	private boolean bigglobe_allowPlacementOnOtherTypesOfEndStones(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.isIn(BigGlobeBlockTags.END_STONES) : state.isOf(block);
	}
}