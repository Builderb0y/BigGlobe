package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record ReverbSoundEffect(
	@DefaultFloat  (AL_REVERB_DEFAULT_AIR_ABSORPTION_GAINHF) @VerifyFloatRange(min = AL_REVERB_MIN_AIR_ABSORPTION_GAINHF, max = AL_REVERB_MAX_AIR_ABSORPTION_GAINHF) float   air_absorption_gainhf,
	@DefaultBoolean(AL_REVERB_DEFAULT_DECAY_HFLIMIT != 0   )                                                                                                         boolean decay_hflimit,
	@DefaultFloat  (AL_REVERB_DEFAULT_DECAY_HFRATIO        ) @VerifyFloatRange(min = AL_REVERB_MIN_DECAY_HFRATIO,         max = AL_REVERB_MAX_DECAY_HFRATIO        ) float   decay_hfratio,
	@DefaultFloat  (AL_REVERB_DEFAULT_DECAY_TIME           ) @VerifyFloatRange(min = AL_REVERB_MIN_DECAY_TIME,            max = AL_REVERB_MAX_DECAY_TIME           ) float   decay_time,
	@DefaultFloat  (AL_REVERB_DEFAULT_DENSITY              ) @VerifyFloatRange(min = AL_REVERB_MIN_DENSITY,               max = AL_REVERB_MAX_DENSITY              ) float   density,
	@DefaultFloat  (AL_REVERB_DEFAULT_DIFFUSION            ) @VerifyFloatRange(min = AL_REVERB_MIN_DIFFUSION,             max = AL_REVERB_MAX_DIFFUSION            ) float   diffusion,
	@DefaultFloat  (AL_REVERB_DEFAULT_GAIN                 ) @VerifyFloatRange(min = AL_REVERB_MIN_GAIN,                  max = AL_REVERB_MAX_GAIN                 ) float   gain,
	@DefaultFloat  (AL_REVERB_DEFAULT_GAINHF               ) @VerifyFloatRange(min = AL_REVERB_MIN_GAINHF,                max = AL_REVERB_MAX_GAINHF               ) float   gainhf,
	@DefaultFloat  (AL_REVERB_DEFAULT_LATE_REVERB_DELAY    ) @VerifyFloatRange(min = AL_REVERB_MIN_LATE_REVERB_DELAY,     max = AL_REVERB_MAX_LATE_REVERB_DELAY    ) float   late_reverb_delay,
	@DefaultFloat  (AL_REVERB_DEFAULT_LATE_REVERB_GAIN     ) @VerifyFloatRange(min = AL_REVERB_MIN_LATE_REVERB_GAIN,      max = AL_REVERB_MAX_LATE_REVERB_GAIN     ) float   late_reverb_gain,
	@DefaultFloat  (AL_REVERB_DEFAULT_REFLECTIONS_DELAY    ) @VerifyFloatRange(min = AL_REVERB_MIN_REFLECTIONS_DELAY,     max = AL_REVERB_MAX_REFLECTIONS_DELAY    ) float   reflections_delay,
	@DefaultFloat  (AL_REVERB_DEFAULT_REFLECTIONS_GAIN     ) @VerifyFloatRange(min = AL_REVERB_MIN_REFLECTIONS_GAIN,      max = AL_REVERB_MAX_REFLECTIONS_GAIN     ) float   reflections_gain,
	@DefaultFloat  (AL_REVERB_DEFAULT_ROOM_ROLLOFF_FACTOR  ) @VerifyFloatRange(min = AL_REVERB_MIN_ROOM_ROLLOFF_FACTOR,   max = AL_REVERB_MAX_ROOM_ROLLOFF_FACTOR  ) float   room_rolloff_factor
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,                  AL_EFFECT_REVERB          );
			alEffectf(resource.effect, AL_REVERB_AIR_ABSORPTION_GAINHF, this.air_absorption_gainhf);
			alEffecti(resource.effect, AL_REVERB_DECAY_HFLIMIT,         this.decay_hflimit ? 1 : 0);
			alEffectf(resource.effect, AL_REVERB_DECAY_HFRATIO,         this.decay_hfratio        );
			alEffectf(resource.effect, AL_REVERB_DECAY_TIME,            this.decay_time           );
			alEffectf(resource.effect, AL_REVERB_DENSITY,               this.density              );
			alEffectf(resource.effect, AL_REVERB_DIFFUSION,             this.diffusion            );
			alEffectf(resource.effect, AL_REVERB_GAIN,                  this.gain                 );
			alEffectf(resource.effect, AL_REVERB_GAINHF,                this.gainhf               );
			alEffectf(resource.effect, AL_REVERB_LATE_REVERB_DELAY,     this.late_reverb_delay    );
			alEffectf(resource.effect, AL_REVERB_LATE_REVERB_GAIN,      this.late_reverb_gain     );
			alEffectf(resource.effect, AL_REVERB_REFLECTIONS_DELAY,     this.reflections_delay    );
			alEffectf(resource.effect, AL_REVERB_REFLECTIONS_GAIN,      this.reflections_gain     );
			alEffectf(resource.effect, AL_REVERB_ROOM_ROLLOFF_FACTOR,   this.room_rolloff_factor  );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}