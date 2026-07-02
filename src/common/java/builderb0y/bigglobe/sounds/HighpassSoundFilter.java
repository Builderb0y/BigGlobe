package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record HighpassSoundFilter(
	@DefaultFloat(AL_HIGHPASS_DEFAULT_GAIN  ) @VerifyFloatRange(min = AL_HIGHPASS_MIN_GAIN,   max = AL_HIGHPASS_MAX_GAIN  ) float gain,
	@DefaultFloat(AL_HIGHPASS_DEFAULT_GAINLF) @VerifyFloatRange(min = AL_HIGHPASS_MIN_GAINLF, max = AL_HIGHPASS_MAX_GAINLF) float gainlf
)
implements SoundFilter {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALFilterResource createResource() {
		ALFilterResource resource = SoundFilter.super.createResource();
		if (resource != null) {
			alFilteri(resource.filter, AL_FILTER_TYPE,     AL_FILTER_HIGHPASS);
			alFilterf(resource.filter, AL_HIGHPASS_GAIN,   this.gain         );
			alFilterf(resource.filter, AL_HIGHPASS_GAINLF, this.gainlf       );
		}
		return resource;
	}
}