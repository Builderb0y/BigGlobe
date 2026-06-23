package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.AbstractOreFeature;
import builderb0y.bigglobe.features.OreFeature;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.features.ScriptedOreFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.RandomSelector;

public class MoltenRockBlock extends Block {

	public static final MapCodec<MoltenRockBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(MoltenRockBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public final @VerifyIntRange(min = 1, max = 8) int heat;

	public MoltenRockBlock(Properties settings, int heat) {
		super(settings);
		this.heat = heat;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState updateShape(

		BlockState state,
		LevelReader world,
		ScheduledTickAccess tickView,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random

	) {
		if (
			world instanceof ServerLevel serverWorld &&
			neighborState.getFluidState().is(FluidTags.WATER)
		) {
			serverWorld.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
			if (
				serverWorld.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
				serverWorld.getRandom().nextFloat() < BigGlobeConfig.INSTANCE.get().moltenRockOreificationChance &&
				serverWorld.getRandom().nextInt(8) < this.heat
			) {
				RandomSelector<BlockState> selector = new RandomSelector<>(new Permuter(serverWorld.getRandom().nextLong()));
				ScriptedColumn column = generator.newColumn(world, pos.getX(), pos.getZ(), ColumnUsage.GENERIC.normalHints());
				for (ConfiguredRockReplacerFeature<?> feature : generator.feature_dispatcher.getFlattenedRockReplacers()) {
					if (feature.config() instanceof AbstractOreFeature.Config config) {
						if (config instanceof OreFeature.Config normalConfig) {
							BlockState newState = normalConfig.blocks.runtimeStates.get(BlockStates.STONE);
							if (newState != null) selector.accept(newState, normalConfig.getCoreChance().get(column, pos.getY()));
						}
						else if (config instanceof ScriptedOreFeature.Config scriptedConfig) {
							BlockState newState = scriptedConfig.replacer_script.getReplacement(
								column,
								BlockStates.STONE,
								pos.getX(),
								pos.getY(),
								pos.getZ(),
								serverWorld.getRandom().nextLong(),
								pos.getX(),
								pos.getY(),
								pos.getZ(),
								1.0D,
								serverWorld.getRandom().nextDouble()
							);
							if (newState != null) selector.accept(newState, scriptedConfig.getCoreChance().get(column, pos.getY()));
						}
					}
				}
				if (selector.value != null) return selector.value;
			}
			return BlockStates.STONE;
		}
		return state;
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(world, pos, state, entity);
		if (
			world instanceof ServerLevel serverWorld &&
			entity instanceof LivingEntity &&
			world.getRandom().nextInt((10 - this.heat) * 10) == 0
		) {
			entity.hurtServer(serverWorld, world.damageSources().hotFloor(), this.heat * 0.5F);
		}
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
		return 1.0F;
	}
}