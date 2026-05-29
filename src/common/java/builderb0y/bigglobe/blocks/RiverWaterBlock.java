package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.GameruleVersions;

@AddPseudoField("fluid")
public class RiverWaterBlock extends LiquidBlock {

	public static final MapCodec<RiverWaterBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RiverWaterBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public RiverWaterBlock(Holder<Fluid> fluid, Properties settings) {
		super((FlowingFluid)(fluid.value()), settings);
	}

	@SuppressWarnings("deprecation")
	public Holder<Fluid> fluid() {
		return this.fluid.builtInRegistryHolder();
	}

	public boolean isDangerous(Level world) {
		if (world instanceof ServerLevel serverWorld) {
			return GameruleVersions.dangerousRapids(serverWorld);
		}
		else {
			ClientState state = ClientState.get(world);
			return state != null && state.dangerousRapids;
		}
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void entityInside(
		BlockState state,
		Level world,
		BlockPos pos,
		Entity entity
		, InsideBlockEffectApplier handler
		, boolean movingFastOrBlockPosIsInsideDestinationBox
	) {
		super.entityInside(
			state,
			world,
			pos,
			entity
			, handler
			, movingFastOrBlockPosIsInsideDestinationBox
		);
		if (
			movingFastOrBlockPosIsInsideDestinationBox &&
			this.isDangerous(world) &&
			!(
				entity instanceof Player player &&
				player.getAbilities().flying
			)
			&& entity.blockPosition().equals(pos)) {
			BlockPos.MutableBlockPos mutablePos = pos.mutable();
			while (world.getBlockState(mutablePos.setY(mutablePos.getY() + 1)).getBlock() == this) ;
			Vec3 velocity = this.fluid.getFlow(world, mutablePos.setY(mutablePos.getY() - 1), world.getFluidState(mutablePos));
			//adding velocity to velocity is normally incredibly dangerous,
			//since this can result in exponential growth.
			//however, the water naturally slows you down, preventing this growth.
			//so, why do I still add it to itself anyway?
			//it's so that the change to velocity is slightly smoother.
			entity.push(
				world.getRandom().triangle((velocity.x + entity.getDeltaMovement().x) * 0.125D, 0.125D),
				world.getRandom().triangle((velocity.y + entity.getDeltaMovement().y) * 0.125D, 0.25D),
				world.getRandom().triangle((velocity.z + entity.getDeltaMovement().z) * 0.125D, 0.125D)
			);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		super.animateTick(state, world, pos, random);
		ClientState clientState;
		if (world.isClientSide() && (clientState = ClientState.get(world)) != null && clientState.dangerousRapids) {
			if (random.nextInt(64) == 0) {
				world.playSound(Minecraft.getInstance().player, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 4.0F, random.nextFloat() + 0.5F);
			}
			if (world.getBlockState(pos.above()).getBlock() != this) {
				//world.addParticle() has an overload which takes particle velocity,
				//but falling water particles in particular don't use the velocity parameter.
				//so, I have to set the velocity manually.
				Particle particle = Minecraft.getInstance().particleEngine.createParticle(
					ParticleTypes.FALLING_WATER,
					pos.getX() + random.nextDouble(),
					pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(),
					0.0D,
					0.0D,
					0.0D
				);
				if (particle != null) {
					Vec3 velocity = world.getFluidState(pos).getFlow(world, pos);
					particle.setParticleSpeed(velocity.x * 0.25D, (velocity.y + random.nextDouble()) * 0.25D, velocity.z * 0.25D);
				}
			}
		}
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (
			state.getValue(LEVEL) == 0 &&
			context instanceof EntityCollisionContext entityContext &&
			entityContext.getEntity() instanceof LivingEntity livingEntity
		) {
			VoxelShape shape = livingEntity.getLiquidCollisionShape();
			if (
				context.isAbove(shape, pos, true) &&
				context.canStandOnFluid(level.getFluidState(pos.above()), state.getFluidState())
			) {
				return shape;
			}
		}
		return Shapes.empty();
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return false;
	}

	@Override
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		//no-op.
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		//no-op.
	}

	@Override
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
		return state;
	}

	@Override
	public ItemStack pickupBlock(@Nullable LivingEntity drainer, LevelAccessor world, BlockPos pos, BlockState state) {
		//don't set block to air.
		return state.getValue(LEVEL) == 0 ? new ItemStack(this.fluid.getBucket()) : ItemStack.EMPTY;
	}
}