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
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class ChorusNyliumBlock extends Block implements BonemealableBlock {

	public static final ResourceKey<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif>
		FEATURE_KEY = ResourceKey.create(
			Registries.#if MC_VERSION >= MC_26_3_0 FEATURE #else CONFIGURED_FEATURE #endif,
			BigGlobeMod.modID("patch_chorus_spores")
		);

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<ChorusNyliumBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ChorusNyliumBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public ChorusNyliumBlock(Properties settings) {
		super(settings);
	}

	@Override
	public boolean isValidBonemealTarget(
		LevelReader world,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(
		Level world,
		RandomSource random,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		return true;
	}

	@Override
	public void performBonemeal(
		ServerLevel world,
		RandomSource random,
		BlockPos pos,
		BlockState state
		#if MC_VERSION >= MC_26_3_0 , BonemealSource source #endif
	) {
		#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif feature = (
			RegistryVersions.getObject(world.registryAccess(), FEATURE_KEY)
		);
		if (feature != null) feature.place(world, world.getChunkSource().getGenerator(), random, pos.above());
	}
}