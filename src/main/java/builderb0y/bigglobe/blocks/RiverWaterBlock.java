package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

public class RiverWaterBlock extends FluidBlock {

	public RiverWaterBlock(FlowableFluid fluid, Settings settings) {
		super(fluid, settings);
	}

	#if MC_VERSION >= MC_1_20_4

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			throw new UnsupportedOperationException();
		}
	#endif

	public boolean isDangerous(World world) {
		if (world.isClient) {
			return ClientState.dangerousRapids;
		}
		else {
			return world.getGameRules().getBoolean(BigGlobeGameRules.DANGEROUS_RAPIDS);
		}
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		super.onEntityCollision(state, world, pos, entity);
		if (this.isDangerous(world) && !(entity instanceof PlayerEntity player && player.getAbilities().flying) && entity.getBlockPos().equals(pos)) {
			BlockPos.Mutable mutablePos = pos.mutableCopy();
			while (world.getBlockState(mutablePos.setY(mutablePos.getY() + 1)).getBlock() == this);
			Vec3d velocity = this.fluid.getVelocity(world, mutablePos.setY(mutablePos.getY() - 1), world.getFluidState(mutablePos));
			//adding velocity to velocity is normally incredibly dangerous,
			//since this can result in exponential growth.
			//however, the water naturally slows you down, preventing this growth.
			//so, why do I still add it to itself anyway?
			//it's so that the change to velocity is slightly smoother.
			entity.addVelocity(
				world.random.nextTriangular((velocity.x + entity.getVelocity().x) * 0.125D, 0.125D),
				world.random.nextTriangular((velocity.y + entity.getVelocity().y) * 0.125D, 0.25D ),
				world.random.nextTriangular((velocity.z + entity.getVelocity().z) * 0.125D, 0.125D)
			);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);
		if (world.isClient && ClientState.dangerousRapids) {
			world.playSound(MinecraftClient.getInstance().player, pos, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 4.0F, random.nextFloat() + 0.5F);
			if (world.getBlockState(pos.up()).getBlock() != this) {
				//world.addParticle() has an overload which takes particle velocity,
				//but falling water particles in particular don't use the velocity parameter.
				//so, I have to set the velocity manually.
				Particle particle = MinecraftClient.getInstance().particleManager.addParticle(
					ParticleTypes.FALLING_WATER,
					pos.getX() + random.nextDouble(),
					pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(),
					0.0D,
					0.0D,
					0.0D
				);
				if (particle != null) {
					Vec3d velocity = world.getFluidState(pos).getVelocity(world, pos);
					particle.setVelocity(velocity.x * 0.25D, (velocity.y + random.nextDouble()) * 0.25D, velocity.z * 0.25D);
				}
			}
		}
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return (
			context.isAbove(COLLISION_SHAPE, pos, true) &&
			context.canWalkOnFluid(world.getFluidState(pos.up()), state.getFluidState())
			? COLLISION_SHAPE
			: VoxelShapes.empty()
		);
	}

	@Override
	public boolean hasRandomTicks(BlockState state) {
		return false;
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		//no-op.
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		//no-op.
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return state;
	}

	@Override
	public ItemStack tryDrainFluid(@Nullable PlayerEntity player, WorldAccess world, BlockPos pos, BlockState state) {
		//don't set block to air.
		return state.get(LEVEL) == 0 ? new ItemStack(this.fluid.getBucketItem()) : ItemStack.EMPTY;
	}
}