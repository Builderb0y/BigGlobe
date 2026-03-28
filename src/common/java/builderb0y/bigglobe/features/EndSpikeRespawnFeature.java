package builderb0y.bigglobe.features;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.RegistryVersions;

public class EndSpikeRespawnFeature extends Feature<SpikeConfiguration> {

	public static final ResourceKey<ConfiguredFeature<?, ?>> DELEGATE_KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE, BigGlobeMod.modID("end/nest_spike"));

	public EndSpikeRespawnFeature(Codec<SpikeConfiguration> configCodec) {
		super(configCodec);
	}

	public EndSpikeRespawnFeature() {
		this(SpikeConfiguration.CODEC);
	}

	public static long getRandomSeed(WorldGenLevel world, int x, int z) {
		return Permuter.permute(world.getSeed() ^ 0x48FA509DA5C2D42DL, x, z);
	}

	@Override
	public boolean place(FeaturePlaceContext<SpikeConfiguration> context) {
		ConfiguredFeature<?, ?> delegate = RegistryVersions.getObject(context.level().registryAccess(), DELEGATE_KEY);
		if (delegate == null) return false;
		BlockPos beamTarget = context.config().getCrystalBeamTarget();
		boolean placedAny = false;
		for (EndSpike spike : context.config().getSpikes()) {
			long seed = getRandomSeed(context.level(), spike.getCenterX(), spike.getCenterZ());
			BlockPos pos = new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ());
			if (delegate.place(context.level(), context.chunkGenerator(), new MojangPermuter(seed), pos)) {
				if (beamTarget != null) {
					List<EndCrystal> entities = context.level().getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(), Predicates.alwaysTrue());
					if (!entities.isEmpty()) {
						for (EndCrystal entity : entities) {
							entity.setBeamTarget(beamTarget);
						}
					}
				}
				placedAny = true;
			}
		}
		return placedAny;
	}
}