package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlock_MakePlaceableOnGravel {

	@ModifyExpressionValue(method = "canSurvive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 1))
	private boolean bigglobe_makePlaceableOnGravel(boolean oldValue, @Local(index = 4) BlockState downState) {
		return oldValue || downState.is(BigGlobeBlockTags.GRAVELS);
	}
}