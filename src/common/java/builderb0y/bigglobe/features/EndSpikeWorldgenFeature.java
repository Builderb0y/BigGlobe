package builderb0y.bigglobe.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.features.EndSpikeWorldgenFeature.EmptyConfig;
import builderb0y.bigglobe.noise.MojangPermuter;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class EndSpikeWorldgenFeature extends FeatureAdapter<EmptyConfig> {

	#if MC_VERSION >= MC_26_3_0

		public EndSpikeWorldgenFeature(EmptyConfig config) {
			super(config);
		}

	#else

		public EndSpikeWorldgenFeature() {
			this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(EmptyConfig.class));
		}

	#endif

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
			#else
				ConfiguredFeature<?, ?> feature = (ConfiguredFeature<?, ?>)(entry.value());
			#endif
			for (EndSpike spike : EndSpikeFeature.getSpikesForLevel(context.level())) {
				if (spike.isCenterWithinChunk(context.origin())) {
					placed |= feature.place(
						context.level(),
						context.chunkGenerator(),
						new MojangPermuter(
							EndSpikeRespawnFeature.getRandomSeed(
								context.level(),
								spike.getCenterX(),
								spike.getCenterZ()
							)
						),
						new BlockPos(
							spike.getCenterX(),
							spike.getHeight(),
							spike.getCenterZ()
						)
					);
				}
			}
		}
		return placed;
	}

	public static record EmptyConfig() implements Config {}
}