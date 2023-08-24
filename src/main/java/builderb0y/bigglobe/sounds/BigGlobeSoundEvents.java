package builderb0y.bigglobe.sounds;

import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeSoundEvents {

	public static final SoundEvent ENTITY_ROCK_THROW = of("entity.rock.throw");

	public static SoundEvent of(String name) {
		Identifier id = BigGlobeMod.modID(name);
		return Registry.register(
			RegistryVersions.soundEvent(),
			id,
			#if MC_VERSION <= MC_1_19_2
				new SoundEvent(id)
			#else
				SoundEvent.of(id)
			#endif
		);
	}

	public static void init() {}
}