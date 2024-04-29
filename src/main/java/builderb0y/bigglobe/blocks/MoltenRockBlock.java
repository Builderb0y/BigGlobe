package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.features.OreFeature;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.RandomSelector;

public class MoltenRockBlock extends Block {

	public final int heat;

	public MoltenRockBlock(Settings settings, int heat) {
		super(settings);
		this.heat = heat;
	}

	#if MC_VERSION >= MC_1_20_4
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			throw new UnsupportedOperationException();
		}
	#endif

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState getStateForNeighborUpdate(
		BlockState state,
		Direction direction,
		BlockState neighborState,
		WorldAccess world,
		BlockPos pos,
		BlockPos neighborPos
	) {
		if (
			world instanceof ServerWorld serverWorld &&
			neighborState.getFluidState().isIn(FluidTags.WATER)
		) {
			serverWorld.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
			if (
				serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
				serverWorld.random.nextInt(8) < this.heat
			) {
				RandomSelector<BlockState> selector = new RandomSelector<>(new Permuter(serverWorld.random.nextLong()));
				ScriptedColumn column = generator.newColumn(world, pos.getX(), pos.getZ(), Purpose.GENERIC);
				for (ConfiguredRockReplacerFeature<?> feature : generator.feature_dispatcher.getFlattenedRockReplacers()) {
					if (feature.config() instanceof OreFeature.Config config) {
						BlockState newState = config.blocks.runtimeStates.get(BlockStates.STONE);
						if (newState != null) selector.accept(newState, config.chance.get(column, pos.getY()));
					}
				}
				if (selector.value != null) return selector.value;
			}
			return BlockStates.STONE;
		}
		return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		super.onSteppedOn(world, pos, state, entity);
		if (
			!world.isClient &&
			entity instanceof LivingEntity living &&
			!EnchantmentHelper.hasFrostWalker(living) &&
			world.random.nextInt((10 - this.heat) * 10) == 0
		) {
			#if MC_VERSION > MC_1_19_2
			entity.damage(world.getDamageSources().hotFloor(), this.heat * 0.5F);
			#else
			entity.damage(DamageSource.HOT_FLOOR, this.heat * 0.5F);
			#endif
		}
	}
}