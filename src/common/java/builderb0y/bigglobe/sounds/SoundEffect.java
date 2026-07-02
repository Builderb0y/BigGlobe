package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import static org.lwjgl.openal.EXTEfx.*;

public interface SoundEffect extends SoundModifier {

	@Override
	@Environment(EnvType.CLIENT)
	public default @Nullable ALEffectResource createResource() {
		if (ALC.getCapabilities().ALC_EXT_EFX) {
			int effect = alGenEffects();
			int slot = alGenAuxiliaryEffectSlots();
			return new ALEffectResource(effect, slot);
		}
		return null;
	}

	@Environment(EnvType.CLIENT)
	public static class ALEffectResource implements ALResource {

		public int effect, slot;

		public ALEffectResource(int effect, int slot) {
			this.effect = effect;
			this.slot = slot;
		}

		@Override
		public void applyTo(int handle) {
			AL11.alSource3i(handle, AL_AUXILIARY_SEND_FILTER, this.slot, 0, AL_FILTER_NULL);
		}

		@Override
		public void close() {
			int effect = this.effect;
			if (effect >= 0) {
				this.effect = -1;
				alDeleteEffects(effect);
			}
			int slot = this.slot;
			if (slot >= 0) {
				this.slot = -1;
				alDeleteAuxiliaryEffectSlots(slot);
			}
		}
	}
}