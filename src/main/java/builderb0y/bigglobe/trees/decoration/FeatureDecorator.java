package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.FeatureColumns;
import builderb0y.bigglobe.chunkgen.FeatureColumns.ColumnSupplier;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.trees.TreeGenerator;

public class FeatureDecorator implements BlockDecorator {

	public final RegistryKey<ConfiguredFeature<?, ?>> feature;

	public FeatureDecorator(RegistryKey<ConfiguredFeature<?, ?>> feature) {
		this.feature = feature;
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		ConfiguredFeature<?, ?> feature = generator.worldQueue.getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).get(this.feature);
		if (feature != null) {
			ColumnSupplier oldSupplier = FeatureColumns.FEATURE_COLUMNS.get();
			try {
				FeatureColumns.FEATURE_COLUMNS.set(ColumnSupplier.varyingPosition(generator.anywhereColumn));
				feature.generate(
					generator.worldQueue,
					((ServerChunkManager)(generator.worldQueue.getChunkManager())).getChunkGenerator(),
					generator.random.mojang(),
					pos
				);
			}
			finally {
				FeatureColumns.FEATURE_COLUMNS.set(oldSupplier);
			}
		}
		else {
			Identifier id = generator.worldQueue.getRegistryManager().get(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY).getId(generator.palette);
			BigGlobeMod.LOGGER.warn("No feature found for ID " + this.feature.getValue() + "; used by tree with ID: " + id);
		}
	}
}