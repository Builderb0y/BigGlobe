package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;

import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

#if MC_VERSION >= MC_26_2_0
	@Mixin(net.minecraft.client.renderer.extract.LevelExtractor.class)
#else
	@Mixin(LevelRenderer.class)
#endif
public class WorldRenderer_HoldLodSystem implements LodSystemHolder {

	@Unique
	private LodSystem bigglobe_lodSystem;

	@Shadow
	private @Nullable ClientLevel level;

	@Override
	public @Nullable LodSystem bigglobe_getLodSystem() {
		return this.bigglobe_lodSystem;
	}

	@Override
	public void bigglobe_setLodSystem(@Nullable LodSystem system) {
		this.bigglobe_lodSystem = system;
	}

	@Inject(method = "allChanged()V", at = @At("HEAD"))
	private void bigglobe_reloadLods(CallbackInfo callback) {
		LodSystem.reload(this, this.level);
	}

	@Inject(method = "setLevel", at = @At("HEAD"))
	private void bigglobe_closeLodSystemOnWorldChange(ClientLevel world, CallbackInfo callback) {
		this.bigglobe_closeLodSystem(null);
	}

	@Inject(
		#if MC_VERSION >= MC_26_2_0
			method = "onResourceManagerReload",
		#else
			method = "close",
		#endif
		at = @At("HEAD")
	)
	private void bigglobe_closeLodSystem(CallbackInfo callback) {
		if (this.bigglobe_lodSystem != null) {
			this.bigglobe_lodSystem.close();
			this.bigglobe_lodSystem = null;
		}
	}
}