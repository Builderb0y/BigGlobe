package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record EqualizerSoundEffect(
	@DefaultFloat(AL_EQUALIZER_DEFAULT_HIGH_CUTOFF) @VerifyFloatRange(min = AL_EQUALIZER_MIN_HIGH_CUTOFF, max = AL_EQUALIZER_MAX_HIGH_CUTOFF) float high_cutoff,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_HIGH_GAIN  ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_HIGH_GAIN,   max = AL_EQUALIZER_MAX_HIGH_GAIN  ) float high_gain,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_LOW_CUTOFF ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_LOW_CUTOFF,  max = AL_EQUALIZER_MAX_LOW_CUTOFF ) float low_cutoff,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_LOW_GAIN   ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_LOW_GAIN,    max = AL_EQUALIZER_MAX_LOW_GAIN   ) float low_gain,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_MID1_CENTER) @VerifyFloatRange(min = AL_EQUALIZER_MIN_MID1_CENTER, max = AL_EQUALIZER_MAX_MID1_CENTER) float mid1_center,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_MID1_GAIN  ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_MID1_GAIN,   max = AL_EQUALIZER_MAX_MID1_GAIN  ) float mid1_gain,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_MID1_WIDTH ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_MID1_WIDTH,  max = AL_EQUALIZER_MAX_MID1_WIDTH ) float mid1_width,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_MID2_CENTER) @VerifyFloatRange(min = AL_EQUALIZER_MIN_MID2_CENTER, max = AL_EQUALIZER_MAX_MID2_CENTER) float mid2_center,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_MID2_GAIN  ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_MID2_GAIN,   max = AL_EQUALIZER_MAX_MID2_GAIN  ) float mid2_gain,
	@DefaultFloat(AL_EQUALIZER_DEFAULT_MID2_WIDTH ) @VerifyFloatRange(min = AL_EQUALIZER_MIN_MID2_WIDTH,  max = AL_EQUALIZER_MAX_MID2_WIDTH ) float mid2_width
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,           AL_EFFECT_EQUALIZER);
			alEffectf(resource.effect, AL_EQUALIZER_HIGH_CUTOFF, this.high_cutoff   );
			alEffectf(resource.effect, AL_EQUALIZER_HIGH_GAIN,   this.high_gain     );
			alEffectf(resource.effect, AL_EQUALIZER_LOW_CUTOFF,  this.low_cutoff    );
			alEffectf(resource.effect, AL_EQUALIZER_LOW_GAIN,    this.low_gain      );
			alEffectf(resource.effect, AL_EQUALIZER_MID1_CENTER, this.mid1_center   );
			alEffectf(resource.effect, AL_EQUALIZER_MID1_GAIN,   this.mid1_gain     );
			alEffectf(resource.effect, AL_EQUALIZER_MID1_WIDTH,  this.mid1_width    );
			alEffectf(resource.effect, AL_EQUALIZER_MID2_CENTER, this.mid2_center   );
			alEffectf(resource.effect, AL_EQUALIZER_MID2_GAIN,   this.mid2_gain     );
			alEffectf(resource.effect, AL_EQUALIZER_MID2_WIDTH,  this.mid2_width    );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}