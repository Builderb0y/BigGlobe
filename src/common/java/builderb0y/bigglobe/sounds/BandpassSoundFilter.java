package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record BandpassSoundFilter(
	@DefaultFloat(AL_BANDPASS_DEFAULT_GAIN  ) @VerifyFloatRange(min = AL_BANDPASS_MIN_GAIN,   max = AL_BANDPASS_MAX_GAIN  ) float gain,
	@DefaultFloat(AL_BANDPASS_DEFAULT_GAINHF) @VerifyFloatRange(min = AL_BANDPASS_MIN_GAINHF, max = AL_BANDPASS_MAX_GAINHF) float gainhf,
	@DefaultFloat(AL_BANDPASS_DEFAULT_GAINLF) @VerifyFloatRange(min = AL_BANDPASS_MIN_GAINLF, max = AL_BANDPASS_MAX_GAINLF) float gainlf
)
implements SoundFilter {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALFilterResource createResource() {
		ALFilterResource resource = SoundFilter.super.createResource();
		if (resource != null) {
			alFilteri(resource.filter, AL_FILTER_TYPE,     AL_FILTER_BANDPASS);
			alFilterf(resource.filter, AL_BANDPASS_GAIN,   this.gain         );
			alFilterf(resource.filter, AL_BANDPASS_GAINHF, this.gainhf       );
			alFilterf(resource.filter, AL_BANDPASS_GAINLF, this.gainlf       );
		}
		return resource;
	}
}