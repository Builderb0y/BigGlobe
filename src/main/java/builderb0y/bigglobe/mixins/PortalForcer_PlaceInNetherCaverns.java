package builderb0y.bigglobe.mixins;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.PortalForcer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeNetherChunkGenerator;
import builderb0y.bigglobe.util.Directions;

/**
vanilla logic likes to place you on the surface of the lava ocean,
which is bad for gameplay. I'm making it so that you spawn in the cavern area, near the top.
*/
@Mixin(PortalForcer.class)
public class PortalForcer_PlaceInNetherCaverns {

	@Shadow @Final private ServerWorld world;

	@Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
	private void bigglobe_overridePortalLocation(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockLocating.Rectangle>> callback) {
		if (this.world.getChunkManager().getChunkGenerator() instanceof BigGlobeNetherChunkGenerator generator) {
			BigGlobeMod.LOGGER.info("Attempting to find nether portal location at high Y level...");
			Direction positiveTangent = switch (axis) {
				case X -> Directions.POSITIVE_X;
				case Z -> Directions.POSITIVE_Z;
				case Y -> throw new IllegalArgumentException("Y axis");
			};
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();
			int minY = generator.getMinimumY();
			int maxY = minY + generator.getWorldHeight();
			for (int y = maxY - 5; y >= minY; y--) {
				if (this.bigglobe_tryPortal(mutablePos.set(pos.getX(), y, pos.getZ()), positiveTangent, callback)) return;
				for (int r = 1; r <= 12; r++) {
					for (int offsetX = -r; offsetX <= r; offsetX++) {
						if (this.bigglobe_tryPortal(mutablePos.set(pos.getX() + offsetX, y, pos.getZ() + r), positiveTangent, callback)) return;
						if (this.bigglobe_tryPortal(mutablePos.set(pos.getX() + offsetX, y, pos.getZ() - r), positiveTangent, callback)) return;
					}
					for (int offsetZ = -r; ++offsetZ < r; offsetZ++) {
						if (this.bigglobe_tryPortal(mutablePos.set(pos.getX() + r, y, pos.getZ() + offsetZ), positiveTangent, callback)) return;
						if (this.bigglobe_tryPortal(mutablePos.set(pos.getX() - r, y, pos.getZ() + offsetZ), positiveTangent, callback)) return;
					}
				}
			}
			BigGlobeMod.LOGGER.error("Unable to find nether portal location at high Y level.");
		}
	}

	public boolean bigglobe_tryPortal(
		BlockPos.Mutable mutablePos,
		Direction positiveTangent,
		CallbackInfoReturnable<Optional<BlockLocating.Rectangle>> callback
	) {
		int x = mutablePos.getX(), y = mutablePos.getY(), z = mutablePos.getZ();
		if (
			this.bigglobe_checkArea(mutablePos) &&
			this.bigglobe_checkArea(mutablePos.set(x, y, z).move(positiveTangent)) &&
			this.bigglobe_checkArea(mutablePos.set(x, y, z).move(positiveTangent, -1)) &&
			this.bigglobe_checkArea(mutablePos.set(x, y, z).move(positiveTangent,  2))
		) {
			BigGlobeMod.LOGGER.info("Found good portal location at " + x + ", " + y + ", " + z);
			Direction.Axis axis = positiveTangent.getAxis();
			this.bigglobe_placeBlocks(mutablePos.set(x, y, z), axis);
			this.bigglobe_placeBlocks(mutablePos.set(x, y, z).move(positiveTangent), axis);
			this.bigglobe_placeBlocks(mutablePos.set(x, y, z).move(positiveTangent, -1), null);
			this.bigglobe_placeBlocks(mutablePos.set(x, y, z).move(positiveTangent,  2), null);
			callback.setReturnValue(Optional.of(new BlockLocating.Rectangle(new BlockPos(x, y + 1, z), 2, 3)));
			return true;
		}
		else {
			return false;
		}
	}

	private boolean bigglobe_checkArea(BlockPos.Mutable pos) {
		if (!this.world.getBlockState(pos).isSolidBlock(this.world, pos)) {
			return false;
		}
		for (int offsetY = 1; offsetY <= 4; offsetY++) {
			pos.setY(pos.getY() + 1);
			BlockState state = this.world.getBlockState(pos);
			if (!(state.getMaterial().isReplaceable() && state.getFluidState().isEmpty())) {
				return false;
			}
		}
		return true;
	}

	private void bigglobe_placeBlocks(BlockPos.Mutable pos, Direction.Axis axis) {
		this.world.setBlockState(pos, BlockStates.OBSIDIAN, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		for (int y = 1; y <= 3; y++) {
			this.world.setBlockState(pos.setY(pos.getY() + 1), axis == null ? BlockStates.OBSIDIAN : Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}
		this.world.setBlockState(pos.setY(pos.getY() + 1), BlockStates.OBSIDIAN, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
	}
}