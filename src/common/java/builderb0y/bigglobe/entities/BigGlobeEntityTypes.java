package builderb0y.bigglobe.entities;

import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeEntityTypes {

	static {
		BigGlobeMod.LOGGER.debug("Registering entity types...");
	}

	public static final EntityType<TorchArrowEntity> TORCH_ARROW = register(
		"torch_arrow",
		TorchArrowEntity::new,
		MobCategory.MISC,
		true,
		true,
		false,
		false,
		ImmutableSet.of(),
		EntityDimensions.scalable(0.5F, 0.5F),
		4,
		20
	);
	public static final EntityType<RockEntity> ROCK = register(
		"rock",
		RockEntity::new,
		MobCategory.MISC,
		true,
		true,
		false,
		false,
		ImmutableSet.of(),
		EntityDimensions.scalable(0.5F, 0.5F),
		4,
		20
	);
	public static final EntityType<StringEntity> STRING = register(
		"string",
		StringEntity::new,
		MobCategory.MISC,
		true,
		true,
		false,
		true,
		ImmutableSet.of(),
		EntityDimensions.scalable(0.5F, 0.5F),
		4,
		20
	);
	public static final EntityType<WaypointEntity> WAYPOINT = register(
		"waypoint",
		WaypointEntity::new,
		MobCategory.MISC,
		false,
		false,
		true,
		true,
		ImmutableSet.of(),
		EntityDimensions.scalable(2.0F, 2.0F),
		8,
		Integer.MAX_VALUE
	);

	static {
		BigGlobeMod.LOGGER.debug("Done registering entity types.");
	}

	public static <E extends Entity> EntityType<E> register(
		String name,
		EntityFactory<E> factory,
		MobCategory spawnGroup,
		boolean saveable,
		boolean summonable,
		boolean fireImmune,
		boolean spawnableFarFromPlayer,
		ImmutableSet<Block> canSpawnInside,
		EntityDimensions dimensions,
		int maxTrackDistance,
		int trackTickInterval
	) {
		return register(
			name,
			new EntityType<>(
				factory,
				spawnGroup,
				saveable,
				summonable,
				fireImmune,
				spawnableFarFromPlayer,
				canSpawnInside,
				dimensions,

				1.0F,

				maxTrackDistance,
				trackTickInterval,

				Util.makeDescriptionId("entity", BigGlobeMod.modID(name)),
				Optional.empty(),

				FeatureFlagSet.of()
				, true
			)
		);
	}

	public static <E extends Entity> EntityType<E> register(String name, EntityType<E> type) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, BigGlobeMod.modID(name), type);
	}

	/**
	triggers static initializer.
	*/
	public static void init() {}
}