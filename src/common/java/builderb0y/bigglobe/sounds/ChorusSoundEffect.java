package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record ChorusSoundEffect(
	@DefaultFloat  (AL_CHORUS_DEFAULT_DELAY        ) @VerifyFloatRange(min = AL_CHORUS_MIN_DELAY,    max = AL_CHORUS_MAX_DELAY   ) float   delay,
	@DefaultFloat  (AL_CHORUS_DEFAULT_DEPTH        ) @VerifyFloatRange(min = AL_CHORUS_MIN_DEPTH,    max = AL_CHORUS_MAX_DEPTH   ) float   depth,
	@DefaultFloat  (AL_CHORUS_DEFAULT_FEEDBACK     ) @VerifyFloatRange(min = AL_CHORUS_MIN_FEEDBACK, max = AL_CHORUS_MAX_FEEDBACK) float   feedback,
	@DefaultInt    (AL_CHORUS_DEFAULT_PHASE        ) @VerifyIntRange  (min = AL_CHORUS_MIN_PHASE,    max = AL_CHORUS_MAX_PHASE   ) int     phase,
	@DefaultFloat  (AL_CHORUS_DEFAULT_RATE         ) @VerifyFloatRange(min = AL_CHORUS_MIN_RATE,     max = AL_CHORUS_MAX_RATE    ) float   rate,
	@DefaultBoolean(AL_CHORUS_DEFAULT_WAVEFORM != 0)                                                                               boolean waveform
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,     AL_EFFECT_CHORUS     );
			alEffectf(resource.effect, AL_CHORUS_DELAY,    this.delay           );
			alEffectf(resource.effect, AL_CHORUS_DEPTH,    this.depth           );
			alEffectf(resource.effect, AL_CHORUS_FEEDBACK, this.feedback        );
			alEffecti(resource.effect, AL_CHORUS_PHASE,    this.phase           );
			alEffectf(resource.effect, AL_CHORUS_RATE,     this.rate            );
			alEffecti(resource.effect, AL_CHORUS_WAVEFORM, this.waveform ? 1 : 0);
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}