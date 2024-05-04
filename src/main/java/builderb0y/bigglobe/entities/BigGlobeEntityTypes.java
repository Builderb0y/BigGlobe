package builderb0y.bigglobe.entities;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityType.EntityFactory;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.versions.RegistryVersions;

import net.minecraft.resource.featuretoggle.FeatureSet;

public class BigGlobeEntityTypes {

	static { BigGlobeMod.LOGGER.debug("Registering entity types..."); }

	public static final EntityType<TorchArrowEntity> TORCH_ARROW = register(
		"torch_arrow",
		TorchArrowEntity::new,
		SpawnGroup.MISC,
		true,
		true,
		false,
		false,
		ImmutableSet.of(),
		EntityDimensions.changing(0.5F, 0.5F),
		4,
		20
	);
	public static final EntityType<RockEntity> ROCK = register(
		"rock",
		RockEntity::new,
		SpawnGroup.MISC,
		true,
		true,
		false,
		false,
		ImmutableSet.of(),
		EntityDimensions.changing(0.5F, 0.5F),
		4,
		20
	);
	public static final EntityType<StringEntity> STRING = register(
		"string",
		StringEntity::new,
		SpawnGroup.MISC,
		true,
		true,
		false,
		true,
		ImmutableSet.of(),
		EntityDimensions.changing(0.5F, 0.5F),
		4,
		20
	);
	public static final @Nullable EntityType<WaypointEntity> WAYPOINT = (
		BigGlobeConfig.INSTANCE.get().hyperspaceEnabled
		? register(
			"waypoint",
			WaypointEntity::new,
			SpawnGroup.MISC,
			false,
			false,
			true,
			true,
			ImmutableSet.of(),
			EntityDimensions.changing(2.0F, 2.0F),
			8,
			Integer.MAX_VALUE
		)
		: null
	);

	static { BigGlobeMod.LOGGER.debug("Done registering entity types."); }

	public static <E extends Entity> EntityType<E> register(
		String name,
		EntityFactory<E> factory,
		SpawnGroup spawnGroup,
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
				#if MC_VERSION >= MC_1_20_5
				1.0F,
				#endif
				maxTrackDistance,
				trackTickInterval,
				FeatureSet.empty()
			)
		);
	}

	public static <E extends Entity> EntityType<E> register(String name, EntityType<E> type) {
		return Registry.register(RegistryVersions.entityType(), BigGlobeMod.modID(name), type);
	}

	/** triggers static initializer. */
	public static void init() {}
}