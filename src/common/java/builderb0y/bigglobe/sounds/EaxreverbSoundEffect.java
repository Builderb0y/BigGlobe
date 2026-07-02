package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record EaxreverbSoundEffect(
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_AIR_ABSORPTION_GAINHF) @VerifyFloatRange(min = AL_EAXREVERB_MIN_AIR_ABSORPTION_GAINHF, max = AL_EAXREVERB_MAX_AIR_ABSORPTION_GAINHF) float   air_absorption_gainhf,
	@DefaultBoolean(AL_EAXREVERB_DEFAULT_DECAY_HFLIMIT != 0   )                                                                                                               boolean decay_hflimit,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_DECAY_HFRATIO        ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_DECAY_HFRATIO,         max = AL_EAXREVERB_MAX_DECAY_HFRATIO        ) float   decay_hfratio,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_DECAY_LFRATIO        ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_DECAY_LFRATIO,         max = AL_EAXREVERB_MAX_DECAY_LFRATIO        ) float   decay_lfratio,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_DECAY_TIME           ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_DECAY_TIME,            max = AL_EAXREVERB_MAX_DECAY_TIME           ) float   decay_time,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_DENSITY              ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_DENSITY,               max = AL_EAXREVERB_MAX_DENSITY              ) float   density,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_DIFFUSION            ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_DIFFUSION,             max = AL_EAXREVERB_MAX_DIFFUSION            ) float   diffusion,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_ECHO_DEPTH           ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_ECHO_DEPTH,            max = AL_EAXREVERB_MAX_ECHO_DEPTH           ) float   echo_depth,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_ECHO_TIME            ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_ECHO_TIME,             max = AL_EAXREVERB_MAX_ECHO_TIME            ) float   echo_time,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_GAIN                 ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_GAIN,                  max = AL_EAXREVERB_MAX_GAIN                 ) float   gain,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_GAINHF               ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_GAINHF,                max = AL_EAXREVERB_MAX_GAINHF               ) float   gainhf,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_GAINLF               ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_GAINLF,                max = AL_EAXREVERB_MAX_GAINLF               ) float   gainlf,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_HFREFERENCE          ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_HFREFERENCE,           max = AL_EAXREVERB_MAX_HFREFERENCE          ) float   hfreference,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_LATE_REVERB_DELAY    ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_LATE_REVERB_DELAY,     max = AL_EAXREVERB_MAX_LATE_REVERB_DELAY    ) float   late_reverb_delay,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_LATE_REVERB_GAIN     ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_LATE_REVERB_GAIN,      max = AL_EAXREVERB_MAX_LATE_REVERB_GAIN     ) float   late_reverb_gain,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_LFREFERENCE          ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_LFREFERENCE,           max = AL_EAXREVERB_MAX_LFREFERENCE          ) float   lfreference,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_MODULATION_DEPTH     ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_MODULATION_DEPTH,      max = AL_EAXREVERB_MAX_MODULATION_DEPTH     ) float   modulation_depth,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_MODULATION_TIME      ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_MODULATION_TIME,       max = AL_EAXREVERB_MAX_MODULATION_TIME      ) float   modulation_time,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_REFLECTIONS_DELAY    ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_REFLECTIONS_DELAY,     max = AL_EAXREVERB_MAX_REFLECTIONS_DELAY    ) float   reflections_delay,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_REFLECTIONS_GAIN     ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_REFLECTIONS_GAIN,      max = AL_EAXREVERB_MAX_REFLECTIONS_GAIN     ) float   reflections_gain,
	@DefaultFloat  (AL_EAXREVERB_DEFAULT_ROOM_ROLLOFF_FACTOR  ) @VerifyFloatRange(min = AL_EAXREVERB_MIN_ROOM_ROLLOFF_FACTOR,   max = AL_EAXREVERB_MAX_ROOM_ROLLOFF_FACTOR  ) float   room_rolloff_factor
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,                     AL_EFFECT_EAXREVERB       );
			alEffectf(resource.effect, AL_EAXREVERB_AIR_ABSORPTION_GAINHF, this.air_absorption_gainhf);
			alEffecti(resource.effect, AL_EAXREVERB_DECAY_HFLIMIT,         this.decay_hflimit ? 1 : 0);
			alEffectf(resource.effect, AL_EAXREVERB_DECAY_HFRATIO,         this.decay_hfratio        );
			alEffectf(resource.effect, AL_EAXREVERB_DECAY_LFRATIO,         this.decay_lfratio        );
			alEffectf(resource.effect, AL_EAXREVERB_DECAY_TIME,            this.decay_time           );
			alEffectf(resource.effect, AL_EAXREVERB_DENSITY,               this.density              );
			alEffectf(resource.effect, AL_EAXREVERB_DIFFUSION,             this.diffusion            );
			alEffectf(resource.effect, AL_EAXREVERB_ECHO_DEPTH,            this.echo_depth           );
			alEffectf(resource.effect, AL_EAXREVERB_ECHO_TIME,             this.echo_time            );
			alEffectf(resource.effect, AL_EAXREVERB_GAIN,                  this.gain                 );
			alEffectf(resource.effect, AL_EAXREVERB_GAINHF,                this.gainhf               );
			alEffectf(resource.effect, AL_EAXREVERB_GAINLF,                this.gainlf               );
			alEffectf(resource.effect, AL_EAXREVERB_HFREFERENCE,           this.hfreference          );
			alEffectf(resource.effect, AL_EAXREVERB_LATE_REVERB_DELAY,     this.late_reverb_delay    );
			alEffectf(resource.effect, AL_EAXREVERB_LATE_REVERB_GAIN,      this.late_reverb_gain     );
			alEffectf(resource.effect, AL_EAXREVERB_LFREFERENCE,           this.lfreference          );
			alEffectf(resource.effect, AL_EAXREVERB_MODULATION_DEPTH,      this.modulation_depth     );
			alEffectf(resource.effect, AL_EAXREVERB_MODULATION_TIME,       this.modulation_time      );
			alEffectf(resource.effect, AL_EAXREVERB_REFLECTIONS_DELAY,     this.reflections_delay    );
			alEffectf(resource.effect, AL_EAXREVERB_REFLECTIONS_GAIN,      this.reflections_gain     );
			alEffectf(resource.effect, AL_EAXREVERB_ROOM_ROLLOFF_FACTOR,   this.room_rolloff_factor  );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}