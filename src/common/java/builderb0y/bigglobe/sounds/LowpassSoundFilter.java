package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record LowpassSoundFilter(
	@DefaultFloat(AL_LOWPASS_DEFAULT_GAIN  ) @VerifyFloatRange(min = AL_LOWPASS_MIN_GAIN,   max = AL_LOWPASS_MAX_GAIN  ) float gain,
	@DefaultFloat(AL_LOWPASS_DEFAULT_GAINHF) @VerifyFloatRange(min = AL_LOWPASS_MIN_GAINHF, max = AL_LOWPASS_MAX_GAINHF) float gainhf
)
implements SoundFilter {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALFilterResource createResource() {
		ALFilterResource resource = SoundFilter.super.createResource();
		if (resource != null) {
			alFilteri(resource.filter, AL_FILTER_TYPE,    AL_FILTER_LOWPASS);
			alFilterf(resource.filter, AL_LOWPASS_GAIN,   this.gain        );
			alFilterf(resource.filter, AL_LOWPASS_GAINHF, this.gainhf      );
		}
		return resource;
	}
}