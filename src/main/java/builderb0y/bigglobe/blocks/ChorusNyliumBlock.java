package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class ChorusNyliumBlock extends Block implements Fertilizable {

	public static final RegistryKey<ConfiguredFeature<?, ?>> FEATURE_KEY = RegistryKey.of(RegistryKeyVersions.configuredFeature(), BigGlobeMod.modID("patch_chorus_spores"));

	public ChorusNyliumBlock(Settings settings) {
		super(settings);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
		return true;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		ConfiguredFeature<?, ?> feature = world.getRegistryManager().get(RegistryKeyVersions.configuredFeature()).get(FEATURE_KEY);
		if (feature != null) feature.generate(world, world.getChunkManager().getChunkGenerator(), random, pos.up());
	}
}