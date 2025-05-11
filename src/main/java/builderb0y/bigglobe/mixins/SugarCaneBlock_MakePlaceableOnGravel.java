package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.block.BlockState;
import net.minecraft.block.SugarCaneBlock;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlock_MakePlaceableOnGravel {

	@ModifyExpressionValue(method = "canPlaceAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z", ordinal = 1))
	private boolean bigglobe_makePlaceableOnGravel(boolean oldValue, @Local(index = 4) BlockState downState) {
		return oldValue || downState.isIn(BigGlobeBlockTags.GRAVELS);
	}
}