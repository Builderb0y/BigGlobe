package builderb0y.bigglobe.features;

import java.util.List;

import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.EndSpikeRespawnFeature.EmptyConfig;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class EndSpikeRespawnFeature extends FeatureAdapter<EmptyConfig> {

	public static final Identifier DELEGATE_ID = BigGlobeMod.modID("end/nest_spike");

	#if MC_VERSION >= MC_26_3_0

		public final EndSpikeFeature original;

		public EndSpikeRespawnFeature(EndSpikeFeature original) {
			super(new EmptyConfig());
			this.original = original;
		}

	#else

		public EndSpikeRespawnFeature() {
			super(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(EmptyConfig.class));
		}

	#endif

	public static long getRandomSeed(WorldGenLevel world, int x, int z) {
		return Permuter.permute(world.getSeed() ^ 0x48FA509DA5C2D42DL, x, z);
	}

	@Override
	public boolean place(Context<EmptyConfig> context) {
		boolean placed = false;
		Holder<?> entry = (
			context
			.level()
			.registryAccess()
			.lookupOrThrow(Registries.FEATURE)
			.get(EndSpikeRespawnFeature.DELEGATE_ID)
			.orElse(null)
		);
		if (entry != null) {
			#if MC_VERSION >= MC_26_3_0
				Feature feature = (Feature)(entry.value());
				List<EndSpike> spikes = this.original.spikes();
				boolean invulnerable = this.original.crystalInvulnerable();
				BlockPos target = this.original.crystalBeamTarget().orElse(null);
			#else
				ConfiguredFeature<?, ?> feature = (ConfiguredFeature<?, ?>)(entry.value());
				List<EndSpike> spikes = feature.config().spikes();
				boolean invulnerable = feature.config().crystalInvulnerable();
				BlockPos target = feature.config().crystalBeamTarget().orElse(null);
			#endif
			for (EndSpike spike : spikes) {
				long seed = getRandomSeed(context.level(), spike.getCenterX(), spike.getCenterZ());
				BlockPos pos = new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ());
				if (
					feature.place(
						context.level(),
						context.chunkGenerator(),
						new MojangPermuter(seed),
						pos
					)
				) {
					List<EndCrystal> entities = context.level().getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(), Predicates.alwaysTrue());
					if (!entities.isEmpty()) {
						for (EndCrystal entity : entities) {
							entity.setBeamTarget(target);
							entity.setPermanentlyInvulnerable(invulnerable);
						}
					}
					placed = true;
				}
			}
		}
		return placed;
	}

	public static record EmptyConfig() implements Config {}
}