package builderb0y.bigglobe.sounds;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeSoundEvents {

	public static final SoundEvent ENTITY_ROCK_THROW = of("entity.rock.throw");

	public static SoundEvent of(String name) {
		Identifier id = BigGlobeMod.modID(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void init() {}
}