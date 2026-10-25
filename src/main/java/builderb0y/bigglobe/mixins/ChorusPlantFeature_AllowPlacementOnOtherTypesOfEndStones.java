package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ChorusPlantFeature;

@Mixin(ChorusPlantFeature.class)
public class ChorusPlantFeature_AllowPlacementOnOtherTypesOfEndStones {

	@Redirect(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private boolean bigglobe_allowPlacementOnOtherTypesOfEndStones(BlockState state, Block block) {
		return block == Blocks.END_STONE ? state.is(BigGlobeBlockTags.END_STONES) : state.is(block);
	}
}