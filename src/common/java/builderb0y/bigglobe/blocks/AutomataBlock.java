package builderb0y.bigglobe.blocks;

import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class AutomataBlock extends Block {

	public static final MapCodec<AutomataBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(AutomataBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public static final WeakHashMap<BlockGetter, Long2LongOpenHashMap> BURNOUT_MAP = new WeakHashMap<>();

	public static final int DELAY = 2;

	public final boolean natural;
	//quick references for equality checks.
	//faster than map lookup based property checks.
	public final BlockState full, empty;

	public AutomataBlock(Properties settings, boolean natural) {
		super(settings);
		this.natural = natural;
		this.registerDefaultState(this.defaultBlockState().setValue(BigGlobeBlockStateProperties.AUTOMATA_STATE, 0));
		this.empty = this.defaultBlockState();
		this.full = this.empty.setValue(BigGlobeBlockStateProperties.AUTOMATA_STATE, 2);
	}

	@Override
	public void neighborChanged(
		BlockState state,
		Level world,
		BlockPos pos,
		Block sourceBlock,
		@Nullable Orientation wireOrientation,
		boolean notify
	) {
		if (
			state == this.empty &&
			world instanceof ServerLevel serverWorld &&
			serverWorld.getDirectSignalTo(pos) > 0
		) {
			this.activate(serverWorld, pos, new BlockPos.MutableBlockPos());
		}
	}

	@Override
	public void attack(BlockState state, Level world, BlockPos pos, Player player) {
		super.attack(state, world, pos, player);
		if (this.natural && world instanceof ServerLevel serverWorld) {
			this.activate(serverWorld, pos, new BlockPos.MutableBlockPos());
		}
	}

	@Override
	public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		super.fallOn(world, state, pos, entity, fallDistance);
		if (this.natural && world instanceof ServerLevel serverWorld && !entity.isSuppressingBounce()) {
			this.activate(serverWorld, pos, new BlockPos.MutableBlockPos());
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(state, world, pos, random);
		switch (state.getValue(BigGlobeBlockStateProperties.AUTOMATA_STATE)) {
			case 0 -> {
				BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
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
				world.scheduleTick(pos, this, DELAY);
			}
		}
	}

	public int countFull(ServerLevel world, BlockPos pos, BlockPos.MutableBlockPos mutablePos) {
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
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onPlace(state, world, pos, oldState, notify);
		world.scheduleTick(pos, this, 2);
	}

	@Override
	public void affectNeighborsAfterRemoval(
		BlockState state,
		ServerLevel world,
		BlockPos pos,

		boolean moved
	) {
		super.affectNeighborsAfterRemoval(state, world, pos, moved);
		if (this.natural) {
			Long2LongOpenHashMap worldMap = BURNOUT_MAP.get(world);
			if (worldMap != null) worldMap.remove(pos.asLong());
		}
	}

	public void activate(ServerLevel world, BlockPos pos, BlockPos.MutableBlockPos mutablePos) {
		if (this.natural) {
			Long2LongOpenHashMap worldMap = BURNOUT_MAP.computeIfAbsent(world, (BlockGetter $) -> new Long2LongOpenHashMap());
			long time = world.getGameTime();
			long deadline = worldMap.put(pos.asLong(), time + 20L);
			if (deadline > time) {
				return;
			}
		}
		this.changeState(world, pos, 2);
		world.scheduleTick(pos, this, DELAY);
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			mutablePos.setZ(pos.getZ() + offsetZ);
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				mutablePos.setX(pos.getX() + offsetX);
				for (int offsetY = -1; offsetY <= 1; offsetY++) {
					if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
					BlockState adjacent = world.getBlockState(mutablePos.setY(pos.getY() + offsetY));
					if (adjacent.getBlock() instanceof AutomataBlock automata && adjacent == automata.empty) {
						world.scheduleTick(mutablePos, automata, DELAY);
					}
				}
			}
		}
	}

	public void changeState(ServerLevel world, BlockPos pos, int toState) {
		world.blockEvent(pos, this, 0, toState);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
		world.setBlock(pos, state.setValue(BigGlobeBlockStateProperties.AUTOMATA_STATE, data), Block.UPDATE_CLIENTS);
		return false;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BigGlobeBlockStateProperties.AUTOMATA_STATE);
	}
}