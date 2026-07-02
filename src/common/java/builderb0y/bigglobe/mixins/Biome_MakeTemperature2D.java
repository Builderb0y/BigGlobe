package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

@Mixin(Biome.class)
public class Biome_MakeTemperature2D {

	@Inject(method = "getHeightAdjustedTemperature", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getY()I", ordinal = 0), cancellable = true)
	private void bigglobe_makeTemperature2D(BlockPos pos, int seaLevel, CallbackInfoReturnable<Float> callback, @Local(name = "adjustedTemperature") float adjustedTemperature) {
		callback.setReturnValue(adjustedTemperature);
	}
}