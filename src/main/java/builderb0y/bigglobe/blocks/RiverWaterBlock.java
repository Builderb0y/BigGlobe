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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
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
import net.minecraft.world.WorldView;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

@AddPseudoField("fluid")
public class RiverWaterBlock extends FluidBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<RiverWaterBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(RiverWaterBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public RiverWaterBlock(RegistryEntry<Fluid> fluid, Settings settings) {
		super((FlowableFluid)(fluid.value()), settings);
	}

	@SuppressWarnings("deprecation")
	public RegistryEntry<Fluid> fluid() {
		return this.fluid.getRegistryEntry();
	}

	public boolean isDangerous(World world) {
		if (world instanceof ServerWorld serverWorld) {
			return serverWorld.getGameRules().getBoolean(BigGlobeGameRules.DANGEROUS_RAPIDS);
		}
		else {
			return ClientState.dangerousRapids;
		}
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity #if MC_VERSION >= MC_1_21_5 , net.minecraft.entity.EntityCollisionHandler handler #endif) {
		super.onEntityCollision(state, world, pos, entity #if MC_VERSION >= MC_1_21_5 , handler #endif);
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
			if (random.nextInt(64) == 0) {
				world.playSound(MinecraftClient.getInstance().player, pos, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 4.0F, random.nextFloat() + 0.5F);
			}
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
	public BlockState getStateForNeighborUpdate(
		#if MC_VERSION >= MC_1_21_2
			BlockState state,
			WorldView world,
			net.minecraft.world.tick.ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
		#else
			BlockState state,
			Direction direction,
			BlockState neighborState,
			WorldAccess world,
			BlockPos pos,
			BlockPos neighborPos
		#endif
	) {
		return state;
	}

	@Override
	public ItemStack tryDrainFluid(#if MC_VERSION >= MC_1_21_5 @Nullable LivingEntity drainer, #elif MC_VERSION >= MC_1_20_2 @Nullable PlayerEntity player, #endif WorldAccess world, BlockPos pos, BlockState state) {
		//don't set block to air.
		return state.get(LEVEL) == 0 ? new ItemStack(this.fluid.getBucketItem()) : ItemStack.EMPTY;
	}
}