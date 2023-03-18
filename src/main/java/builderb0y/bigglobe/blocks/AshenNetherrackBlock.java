package builderb0y.bigglobe.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;

public class AshenNetherrackBlock extends Block implements Fertilizable {

	public static final RegistryKey<ConfiguredFeature<?, ?>> PATCH_CHARRED_GRASS = RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, BigGlobeMod.modID("patch_charred_grass"));

	public AshenNetherrackBlock(Settings settings) {
		super(settings);
	}

	@Override
	public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
		return true;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return world.getBlockState(pos.up()).isAir();
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		ConfiguredFeature<?, ?> feature = world.getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).get(PATCH_CHARRED_GRASS);
		if (feature != null) feature.generate(world, world.getChunkManager().getChunkGenerator(), random, pos.up());
	}
}