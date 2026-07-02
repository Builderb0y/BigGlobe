package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record EchoSoundEffect(
	@DefaultFloat(AL_ECHO_DEFAULT_DAMPING ) @VerifyFloatRange(min = AL_ECHO_MIN_DAMPING,  max = AL_ECHO_MAX_DAMPING ) float damping,
	@DefaultFloat(AL_ECHO_DEFAULT_DELAY   ) @VerifyFloatRange(min = AL_ECHO_MIN_DELAY,    max = AL_ECHO_MAX_DELAY   ) float delay,
	@DefaultFloat(AL_ECHO_DEFAULT_FEEDBACK) @VerifyFloatRange(min = AL_ECHO_MIN_FEEDBACK, max = AL_ECHO_MAX_FEEDBACK) float feedback,
	@DefaultFloat(AL_ECHO_DEFAULT_LRDELAY ) @VerifyFloatRange(min = AL_ECHO_MIN_LRDELAY,  max = AL_ECHO_MAX_LRDELAY ) float lrdelay,
	@DefaultFloat(AL_ECHO_DEFAULT_SPREAD  ) @VerifyFloatRange(min = AL_ECHO_MIN_SPREAD,   max = AL_ECHO_MAX_SPREAD  ) float spread
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,   AL_EFFECT_ECHO);
			alEffectf(resource.effect, AL_ECHO_DAMPING,  this.damping  );
			alEffectf(resource.effect, AL_ECHO_DELAY,    this.delay    );
			alEffectf(resource.effect, AL_ECHO_FEEDBACK, this.feedback );
			alEffectf(resource.effect, AL_ECHO_LRDELAY,  this.lrdelay  );
			alEffectf(resource.effect, AL_ECHO_SPREAD,   this.spread   );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}