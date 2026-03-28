package builderb0y.bigglobe.fluids;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.GameruleVersions;

public abstract class SoulLavaFluid extends FlowingFluid {

	@Override
	public Fluid getFlowing() {
		return BigGlobeFluids.FLOWING_SOUL_LAVA;
	}

	@Override
	public Fluid getSource() {
		return BigGlobeFluids.SOUL_LAVA;
	}

	@Override
	public boolean canConvertToSource(

		ServerLevel world

	) {
		return GameruleVersions.soulLavaSourceConversion(world);
	}

	@Override
	public void beforeDestroyingBlock(LevelAccessor world, BlockPos pos, BlockState state) {
		world.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
	}

	@Override
	public int getSlopeFindDistance(LevelReader world) {
		return this.isNether(world) ? 4 : 2;
	}

	@Override
	public int getDropOff(LevelReader world) {
		return this.isNether(world) ? 1 : 2;
	}

	@Override
	public Item getBucket() {
		return BigGlobeItems.SOUL_LAVA_BUCKET;
	}

	@Override
	@SuppressWarnings("deprecation") //this is what vanilla does.
	public boolean canBeReplacedWith(FluidState state, BlockGetter world, BlockPos pos, Fluid fluid, Direction direction) {
		return state.getHeight(world, pos) >= 0.44444445F && fluid.is(FluidTags.WATER);
	}

	@Override
	public void entityInside(Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler) {
		handler.apply(InsideBlockEffectType.LAVA_IGNITE);
		handler.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
	}

	@Override
	public int getTickDelay(LevelReader world) {
		return this.isNether(world) ? 10 : 30;
	}

	public boolean isNether(LevelReader world) {

		return world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
	}

	@Override
	public float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	public BlockState createLegacyBlock(FluidState state) {
		return BigGlobeBlocks.SOUL_LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == BigGlobeFluids.SOUL_LAVA || fluid == BigGlobeFluids.FLOWING_SOUL_LAVA;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
	}

	public static class Flowing extends SoulLavaFluid {

		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}

	public static class Still extends SoulLavaFluid {

		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}
}