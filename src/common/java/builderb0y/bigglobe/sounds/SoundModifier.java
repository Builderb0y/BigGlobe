package builderb0y.bigglobe.sounds;

import java.io.Closeable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;

@UseCoder(name = "REGISTRY", in = SoundModifier.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface SoundModifier extends CoderRegistryTyped<SoundModifier> {

	public static final CoderRegistry<SoundModifier> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("sound_modifier"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("autowah"   ),    AutowahSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("bandpass"  ),   BandpassSoundFilter.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("chorus"    ),     ChorusSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("compressor"), CompressorSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("distortion"), DistortionSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("eaxreverb" ),  EaxreverbSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("echo"      ),       EchoSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("equalizer" ),  EqualizerSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("flanger"   ),    FlangerSoundEffect.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("highpass"  ),   HighpassSoundFilter.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("lowpass"   ),    LowpassSoundFilter.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("reverb"    ),     ReverbSoundEffect.class);
	}};

	@Environment(EnvType.CLIENT)
	public abstract @Nullable ALResource createResource();

	@Environment(EnvType.CLIENT)
	public static interface ALResource extends Closeable  {

		public void applyTo(int handle);

		@Override
		public void close();
	}
}