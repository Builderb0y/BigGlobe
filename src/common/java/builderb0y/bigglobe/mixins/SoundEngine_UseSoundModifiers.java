package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngine.PlayResult;

import builderb0y.bigglobe.sounds.SoundModifierManager;

@Mixin(SoundEngine.class)
public class SoundEngine_UseSoundModifiers {

	@Unique private final SoundModifierManager bigglobe_soundModifierManager = new SoundModifierManager();

	@Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V", shift = Shift.AFTER))
	private void bigglobe_useSoundModifiers(SoundInstance sound, CallbackInfoReturnable<PlayResult> callback, @Local(name = "handle") ChannelHandle handle) {
		this.bigglobe_soundModifierManager.applyModifiersTo(sound, handle);
	}

	@Inject(method = "destroy", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Library;cleanup()V"))
	private void bigglobe_destroySoundEffects(CallbackInfo callback) {
		this.bigglobe_soundModifierManager.destroy();
	}
}