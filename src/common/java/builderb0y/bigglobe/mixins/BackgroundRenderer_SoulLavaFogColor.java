package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.rendering2.SoulLavaFogHandler;

@Environment(EnvType.CLIENT)
@Mixin(LavaFogEnvironment.class)
public class BackgroundRenderer_SoulLavaFogColor {

	@ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"))
	private int bigglobe_useBlueFogColorInSoulLava(int original) {
		if (SoulLavaFogHandler.inSoulLava) {
			return (original & 0xFF00FF00) | (Integer.rotateLeft(original, 16) & 0x00FF00FF);
		}
		return original;
	}
}