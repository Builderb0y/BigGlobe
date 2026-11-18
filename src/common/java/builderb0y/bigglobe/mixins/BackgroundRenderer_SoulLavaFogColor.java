package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;

import builderb0y.bigglobe.rendering.SoulLavaFogHandler;

@Environment(EnvType.CLIENT)
@Mixin(LavaFogEnvironment.class)
public class BackgroundRenderer_SoulLavaFogColor {

	#if MC_VERSION >= MC_26_3_0

		@ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"))
		private Vector3fc bigglobe_useBlueFogColorInSoulLava(Vector3fc original) {
			if (SoulLavaFogHandler.inSoulLava) {
				return new Vector3f(original.z(), original.y(), original.x());
			}
			else {
				return original;
			}
		}

	#else

		@ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"))
		private int bigglobe_useBlueFogColorInSoulLava(int original) {
			if (SoulLavaFogHandler.inSoulLava) {
				return (original & 0xFF00FF00) | (Integer.rotateLeft(original, 16) & 0x00FF00FF);
			}
			return original;
		}

	#endif
}