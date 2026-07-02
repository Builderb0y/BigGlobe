package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record AutowahSoundEffect(
	@DefaultFloat(AL_AUTOWAH_DEFAULT_ATTACK_TIME ) @VerifyFloatRange(min = AL_AUTOWAH_MIN_ATTACK_TIME,  max = AL_AUTOWAH_MAX_ATTACK_TIME ) float attack_time,
	@DefaultFloat(AL_AUTOWAH_DEFAULT_PEAK_GAIN   ) @VerifyFloatRange(min = AL_AUTOWAH_MIN_PEAK_GAIN,    max = AL_AUTOWAH_MAX_PEAK_GAIN   ) float peak_gain,
	@DefaultFloat(AL_AUTOWAH_DEFAULT_RELEASE_TIME) @VerifyFloatRange(min = AL_AUTOWAH_MIN_RELEASE_TIME, max = AL_AUTOWAH_MAX_RELEASE_TIME) float release_time,
	@DefaultFloat(AL_AUTOWAH_DEFAULT_RESONANCE   ) @VerifyFloatRange(min = AL_AUTOWAH_MIN_RESONANCE,    max = AL_AUTOWAH_MAX_RESONANCE   ) float resonance
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,          AL_EFFECT_AUTOWAH);
			alEffectf(resource.effect, AL_AUTOWAH_ATTACK_TIME,  this.attack_time );
			alEffectf(resource.effect, AL_AUTOWAH_PEAK_GAIN,    this.peak_gain   );
			alEffectf(resource.effect, AL_AUTOWAH_RELEASE_TIME, this.release_time);
			alEffectf(resource.effect, AL_AUTOWAH_RESONANCE,    this.resonance   );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}