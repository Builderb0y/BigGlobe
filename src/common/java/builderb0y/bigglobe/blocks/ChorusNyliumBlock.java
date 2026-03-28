package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.RegistryVersions;

public class ChorusNyliumBlock extends Block implements BonemealableBlock {

	public static final ResourceKey<ConfiguredFeature<?, ?>> FEATURE_KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE, BigGlobeMod.modID("patch_chorus_spores"));

	public static final MapCodec<ChorusNyliumBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ChorusNyliumBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public ChorusNyliumBlock(Properties settings) {
		super(settings);
	}

	@Override
	public boolean isValidBonemealTarget(
		LevelReader world,
		BlockPos pos,
		BlockState state

	) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
		ConfiguredFeature<?, ?> feature = RegistryVersions.getObject(world.registryAccess(), FEATURE_KEY);
		if (feature != null) feature.place(world, world.getChunkSource().getGenerator(), random, pos.above());
	}
}