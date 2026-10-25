package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.fluids.BigGlobeFluidTags;
import builderb0y.bigglobe.rendering.SoulLavaFogHandler;

@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public class Camera_HandleSoulLavaSubmersion {

	@ModifyExpressionValue(
		method = "getFluidInCamera",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/world/level/material/FogType;LAVA:Lnet/minecraft/world/level/material/FogType;",
			opcode = Opcodes.GETSTATIC
		)
	)
	private FogType bigglobe_handleSoulLavaSubmersion(FogType original, @Local(index = 8) FluidState fluidState) {
		SoulLavaFogHandler.inSoulLava = fluidState.is(BigGlobeFluidTags.SOUL_LAVA);
		return original;
	}

	@Inject(method = "getFluidInCamera", at = @At("HEAD"))
	private void bigglobe_resetSoulLavaState(CallbackInfoReturnable<FogType> callback) {
		SoulLavaFogHandler.inSoulLava = false;
	}
}