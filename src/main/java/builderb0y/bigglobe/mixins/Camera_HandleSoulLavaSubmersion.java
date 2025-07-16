package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Camera;
import net.minecraft.fluid.FluidState;

import builderb0y.bigglobe.fluids.BigGlobeFluidTags;
import builderb0y.bigglobe.rendering.SoulLavaFogHandler;

#if MC_VERSION >= MC_1_21_0
	import net.minecraft.block.enums.CameraSubmersionType;
#else
	import net.minecraft.client.render.CameraSubmersionType;
#endif

@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public class Camera_HandleSoulLavaSubmersion {

	@ModifyExpressionValue(
		method = "getSubmersionType",
		at = @At(
			value = "FIELD",
			#if MC_VERSION >= MC_1_21_0
				target = "Lnet/minecraft/block/enums/CameraSubmersionType;LAVA:Lnet/minecraft/block/enums/CameraSubmersionType;"
			#else
				target = "Lnet/minecraft/client/render/CameraSubmersionType;LAVA:Lnet/minecraft/client/render/CameraSubmersionType;"
			#endif
		)
	)
	private CameraSubmersionType bigglobe_handleSoulLavaSubmersion(CameraSubmersionType original, @Local(index = 8) FluidState fluidState) {
		SoulLavaFogHandler.inSoulLava = fluidState.isIn(BigGlobeFluidTags.SOUL_LAVA);
		return original;
	}

	@Inject(method = "getSubmersionType", at = @At("HEAD"))
	private void bigglobe_resetSoulLavaState(CallbackInfoReturnable<CameraSubmersionType> callback) {
		SoulLavaFogHandler.inSoulLava = false;
	}
}