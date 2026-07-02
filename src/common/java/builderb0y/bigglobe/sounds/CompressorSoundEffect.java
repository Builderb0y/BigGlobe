package builderb0y.bigglobe.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;

import builderb0y.autocodec.annotations.*;

import static org.lwjgl.openal.EXTEfx.*;

public record CompressorSoundEffect(
	@DefaultBoolean(AL_COMPRESSOR_DEFAULT_ONOFF != 0)  boolean onoff
)
implements SoundEffect {

	@Override
	@Environment(EnvType.CLIENT)
	public @Nullable ALEffectResource createResource() {
		ALEffectResource resource = SoundEffect.super.createResource();
		if (resource != null) {
			alEffecti(resource.effect, AL_EFFECT_TYPE,      AL_EFFECT_COMPRESSOR);
			alEffecti(resource.effect, AL_COMPRESSOR_ONOFF, this.onoff ? 1 : 0  );
			alAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);
		}
		return resource;
	}
}