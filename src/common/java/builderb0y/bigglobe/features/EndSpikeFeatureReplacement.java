package builderb0y.bigglobe.features;

import java.util.List;

import com.google.common.base.Predicates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class EndSpikeFeatureReplacement {

	public static final Identifier DELEGATE_ID = BigGlobeMod.modID("end/nest_spike");

	public static Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> findDelegate(WorldGenLevel world) {
		return (
			world
			.registryAccess()
			.lookupOrThrow(
				#if MC_VERSION >= MC_26_3_0
					Registries.FEATURE
				#else
					Registries.CONFIGURED_FEATURE
				#endif
			)
			.get(DELEGATE_ID)
			.orElse(null)
		);
	}

	public static long getRandomSeed(WorldGenLevel world, int x, int z) {
		return Permuter.permute(world.getSeed() ^ 0x48FA509DA5C2D42DL, x, z);
	}

	public static boolean place(
		WorldGenLevel world,
		ChunkGenerator generator,
		EndSpike spike,
		BlockPos target,
		boolean invulnerable
	) {
		Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> feature = findDelegate(world);
		if (feature != null) {
			long seed = getRandomSeed(world, spike.getCenterX(), spike.getCenterZ());
			BlockPos pos = new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ());
			if (
				feature.value().place(
					world,
					generator,
					new MojangPermuter(seed),
					pos
				)
			) {
				List<EndCrystal> entities = world.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(), Predicates.alwaysTrue());
				if (!entities.isEmpty()) {
					for (EndCrystal entity : entities) {
						entity.setBeamTarget(target);
						#if MC_VERSION >= MC_26_3_0
							entity.setPermanentlyInvulnerable(invulnerable);
						#else
							entity.setInvulnerable(invulnerable);
						#endif
					}
				}
				return true;
			}
		}
		return false;
	}
}