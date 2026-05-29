package builderb0y.bigglobe.sounds;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeSoundEvents {

	public static final SoundEvent ENTITY_ROCK_THROW = of("entity.rock.throw");
	public static final Holder<SoundEvent> ITEM_ARMOR_EQUIP_VOIDMETAL = entryOf("item.armor.equip_voidmetal");

	public static SoundEvent of(String name) {
		Identifier id = BigGlobeMod.modID(name);
		return Registry.register(
			BuiltInRegistries.SOUND_EVENT,
			id,
			SoundEvent.createVariableRangeEvent(id)
		);
	}

	public static Holder<SoundEvent> entryOf(String name) {
		Identifier id = BigGlobeMod.modID(name);
		return Registry.registerForHolder(
			BuiltInRegistries.SOUND_EVENT,
			id,
			SoundEvent.createVariableRangeEvent(id)
		);
	}

	public static void init() {}
}