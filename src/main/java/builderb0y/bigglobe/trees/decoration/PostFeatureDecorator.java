package builderb0y.bigglobe.trees.decoration;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;

public class PostFeatureDecorator implements TrunkDecorator {

	public final RegistryEntry<ConfiguredFeature<?, ?>> feature;

	public PostFeatureDecorator(RegistryEntry<ConfiguredFeature<?, ?>> feature) {
		this.feature = feature;
	}

	@Override
	public void decorate(TreeGenerator generator, TrunkConfig trunk) {
		this.feature.value().generate(
			generator.worldQueue,
			((ServerChunkManager)(generator.worldQueue.getChunkManager())).getChunkGenerator(),
			generator.random.mojang(),
			new BlockPos(
				BigGlobeMath.roundI(trunk.startX),
				trunk.startY,
				BigGlobeMath.roundI(trunk.startZ)
			)
		);
	}
}