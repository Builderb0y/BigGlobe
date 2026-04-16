package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;

public abstract class AutomataBlock extends Block {

	//quick references for equality checks.
	//faster than map lookup based property checks.
	public final BlockState full, medium, empty;

	public AutomataBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(BigGlobeBlockStateProperties.AUTOMATA_STATE, 0));
		this.empty = this.defaultBlockState();
		this.medium = this.empty.setValue(BigGlobeBlockStateProperties.AUTOMATA_STATE, 1);
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
			!(sourceBlock instanceof AutomataBlock) &&
			world instanceof ServerLevel serverWorld &&
			serverWorld.getDirectSignalTo(pos) > 0
		) {
			this.activate(serverWorld, pos);
		}
	}

	public abstract boolean canActivateFromSpread(ServerLevel world, BlockPos pos, MutableBlockPos reuse);

	public static int countFull(ServerLevel world, BlockPos pos, MutableBlockPos mutablePos, int limit) {
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
						if (count >= limit) break outer;
					}
				}
			}
		}
		return count;
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onPlace(state, world, pos, oldState, notify);
		if (world instanceof ServerLevel serverWorld && !oldState.is(this)) {
			if (world.getDirectSignalTo(pos) > 0) {
				this.activate(serverWorld, pos);
			}
			else if (state != this.empty) {
				world.setBlock(pos, this.empty, Block.UPDATE_ALL);
			}
		}
	}

	public abstract void activate(ServerLevel world, BlockPos pos);

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BigGlobeBlockStateProperties.AUTOMATA_STATE);
	}
}