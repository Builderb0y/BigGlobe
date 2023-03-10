package builderb0y.bigglobe.entities;

import com.google.common.collect.ImmutableSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeEntityTypes {

	static { BigGlobeMod.LOGGER.debug("Registering entity types..."); }

	public static final EntityType<TorchArrowEntity> TORCH_ARROW = register(
		"torch_arrow",
		new EntityType<>(
			TorchArrowEntity::new,                 //factory
			SpawnGroup.MISC,                       //spawnGroup
			true,                                  //saveable
			true,                                  //summonable
			false,                                 //fireImmune
			false,                                 //spawnableFarFromPlayer
			ImmutableSet.of(),                     //canSpawnInside
			EntityDimensions.changing(0.5F, 0.5F), //dimensions
			4,                                     //maxTrackingRange
			20                                     //trackingInterval
		)
	);

	static { BigGlobeMod.LOGGER.debug("Done registering entity types."); }

	public static <E extends Entity> EntityType<E> register(String name, EntityType<E> type) {
		return Registry.register(Registry.ENTITY_TYPE, BigGlobeMod.modID(name), type);
	}

	/** triggers static initializer. */
	public static void init() {}
}