package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import static org.lwjgl.openal.EXTEfx.*;

public interface SoundFilter extends SoundModifier {

	@Override
	@Environment(EnvType.CLIENT)
	public default @Nullable ALFilterResource createResource() {
		if (ALC.getCapabilities().ALC_EXT_EFX) {
			int filter = alGenFilters();
			return new ALFilterResource(filter);
		}
		return null;
	}

	@Environment(EnvType.CLIENT)
	public static class ALFilterResource implements ALResource {

		public int filter;

		public ALFilterResource(int filter) {
			this.filter = filter;
		}

		@Override
		public void applyTo(int handle) {
			AL11.alSourcei(handle, AL_DIRECT_FILTER, this.filter);
		}

		@Override
		public void close() {
			int filter = this.filter;
			if (filter >= 0) {
				this.filter = -1;
				alDeleteFilters(filter);
			}
		}
	}
}