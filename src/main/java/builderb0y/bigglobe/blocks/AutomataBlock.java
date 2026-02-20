package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class AutomataBlock extends Block {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<AutomataBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(AutomataBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public static final WeakHashMap<BlockView, Long2LongOpenHashMap> BURNOUT_MAP = new WeakHashMap<>();

	public static final int DELAY = 2;

	public final boolean natural;
	//quick references for equality checks.
	//faster than map lookup based property checks.
	public final BlockState full, empty;

	public AutomataBlock(Settings settings, boolean natural) {
		super(settings);
		this.natural = natural;
		this.setDefaultState(this.getDefaultState().with(BigGlobeBlockStateProperties.AUTOMATA_STATE, 0));
		this.empty = this.getDefaultState();
		this.full = this.empty.with(BigGlobeBlockStateProperties.AUTOMATA_STATE, 2);
	}

	@Override
	public void neighborUpdate(
		#if MC_VERSION >= MC_1_21_3
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			@Nullable net.minecraft.world.block.WireOrientation wireOrientation,
			boolean notify
		#else
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			BlockPos sourcePos,
			boolean notify
		#endif
	) {
		if (
			state == this.empty &&
			world instanceof ServerWorld serverWorld &&
			serverWorld.getReceivedStrongRedstonePower(pos) > 0
		) {
			this.activate(serverWorld, pos, new BlockPos.Mutable());
		}
	}

	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		super.onBlockBreakStart(state, world, pos, player);
		if (this.natural && world instanceof ServerWorld serverWorld) {
			this.activate(serverWorld, pos, new BlockPos.Mutable());
		}
	}

	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		super.onLandedUpon(world, state, pos, entity, fallDistance);
		if (this.natural && world instanceof ServerWorld serverWorld && !entity.bypassesLandingEffects()) {
			this.activate(serverWorld, pos, new BlockPos.Mutable());
		}
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		super.scheduledTick(state, world, pos, random);
		this.tick(world, pos, state, random);
	}

	public void tick(ServerWorld world, BlockPos pos, BlockState state, Random random) {
		switch (state.get(BigGlobeBlockStateProperties.AUTOMATA_STATE)) {
			case 0 -> {
				BlockPos.Mutable mutablePos = new BlockPos.Mutable();
				int count = this.countFull(world, pos, mutablePos);
				if (count == 1 || count == 2) {
					this.activate(world, pos, mutablePos);
				}
			}
			case 1 -> {
				this.changeState(world, pos, 0);
			}
			case 2 -> {
				this.changeState(world, pos, 1);
				world.scheduleBlockTick(pos, this, DELAY);
			}
		}
	}

	public int countFull(ServerWorld world, BlockPos pos, BlockPos.Mutable mutablePos) {
		int count = 0;
		outer:
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			mutablePos.setZ(pos.getZ() + offsetZ);
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				mutablePos.setX(pos.getX() + offsetX);
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					BlockState adjacent = world.getBlockState(mutablePos.setY(pos.getY() + offsetY));
					if (adjacent.getBlock() instanceof AutomataBlock automata && adjacent == automata.full) {
						count++;
						if (this.natural || count >= 3) break outer;
					}
				}
			}
		}
		return count;
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		world.scheduleBlockTick(pos, this, 2);
	}

	@Override
	public void onStateReplaced(
		BlockState state,
		#if MC_VERSION >= MC_1_21_5 ServerWorld #else World #endif world,
		BlockPos pos,
		#if MC_VERSION < MC_1_21_5 BlockState newState, #endif
		boolean moved
	) {
		super.onStateReplaced(state, world, pos, #if MC_VERSION < MC_1_21_5 newState, #endif moved);
		if (this.natural) {
			Long2LongOpenHashMap worldMap = BURNOUT_MAP.get(world);
			if (worldMap != null) worldMap.remove(pos.asLong());
		}
	}

	public void activate(ServerWorld world, BlockPos pos, BlockPos.Mutable mutablePos) {
		if (this.natural) {
			Long2LongOpenHashMap worldMap = BURNOUT_MAP.computeIfAbsent(world, (BlockView $) -> new Long2LongOpenHashMap());
			long time = world.getTime();
			long deadline = worldMap.put(pos.asLong(), time + 20L);
			if (deadline > time) {
				return;
			}
		}
		this.changeState(world, pos, 2);
		world.scheduleBlockTick(pos, this, DELAY);
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			mutablePos.setZ(pos.getZ() + offsetZ);
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				mutablePos.setX(pos.getX() + offsetX);
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
					BlockState adjacent = world.getBlockState(mutablePos.setY(pos.getY() + offsetY));
					if (adjacent.getBlock() instanceof AutomataBlock automata && adjacent == automata.empty) {
						world.scheduleBlockTick(mutablePos, automata, DELAY);
					}
				}
			}
		}
	}

	public void changeState(ServerWorld world, BlockPos pos, int toState) {
		world.addSyncedBlockEvent(pos, this, 0, toState);
	}

	@Override
	public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
		world.setBlockState(pos, state.with(BigGlobeBlockStateProperties.AUTOMATA_STATE, data), Block.NOTIFY_LISTENERS);
		return false;
	}

	@Override
	public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(BigGlobeBlockStateProperties.AUTOMATA_STATE);
	}
}