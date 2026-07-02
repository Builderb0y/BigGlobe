package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record DistortionSoundEffect(
	@DefaultFloat(AL_DISTORTION_DEFAULT_EDGE          ) @VerifyFloatRange(min = AL_DISTORTION_MIN_EDGE,           max = AL_DISTORTION_MAX_EDGE          ) float edge,
	@DefaultFloat(AL_DISTORTION_DEFAULT_EQBANDWIDTH   ) @VerifyFloatRange(min = AL_DISTORTION_MIN_EQBANDWIDTH,    max = AL_DISTORTION_MAX_EQBANDWIDTH   ) float eqbandwidth,
	@DefaultFloat(AL_DISTORTION_DEFAULT_EQCENTER      ) @VerifyFloatRange(min = AL_DISTORTION_MIN_EQCENTER,       max = AL_DISTORTION_MAX_EQCENTER      ) float eqcenter,
	@DefaultFloat(AL_DISTORTION_DEFAULT_GAIN          ) @VerifyFloatRange(min = AL_DISTORTION_MIN_GAIN,           max = AL_DISTORTION_MAX_GAIN          ) float gain,
	@DefaultFloat(AL_DISTORTION_DEFAULT_LOWPASS_CUTOFF) @VerifyFloatRange(min = AL_DISTORTION_MIN_LOWPASS_CUTOFF, max = AL_DISTORTION_MAX_LOWPASS_CUTOFF) float lowpass_cutoff
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,               AL_EFFECT_DISTORTION);
			alEffectf(resource.effect, AL_DISTORTION_EDGE,           this.edge           );
			alEffectf(resource.effect, AL_DISTORTION_EQBANDWIDTH,    this.eqbandwidth    );
			alEffectf(resource.effect, AL_DISTORTION_EQCENTER,       this.eqcenter       );
			alEffectf(resource.effect, AL_DISTORTION_GAIN,           this.gain           );
			alEffectf(resource.effect, AL_DISTORTION_LOWPASS_CUTOFF, this.lowpass_cutoff );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}