package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.WorldRenderer;

import builderb0y.bigglobe.lods.LodSystem;

@Mixin(WorldRenderer.class)
public class WorldRenderer_ReloadHook {

	@Inject(method = "reload()V", at = @At("HEAD"))
	private void bigglobe_reloadLods(CallbackInfo ci) {
		LodSystem.reload();
	}
}