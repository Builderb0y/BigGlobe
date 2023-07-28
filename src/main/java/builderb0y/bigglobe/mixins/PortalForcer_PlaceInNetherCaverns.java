package builderb0y.bigglobe.mixins;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.BlockLocating.Rectangle;
import net.minecraft.world.PortalForcer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeNetherChunkGenerator;
import builderb0y.bigglobe.util.NetherPortalUtil;

/**
vanilla logic likes to place you on the surface of the lava ocean,
which is bad for gameplay. I'm making it so that you spawn in the cavern area, near the top.
*/
@Mixin(PortalForcer.class)
public class PortalForcer_PlaceInNetherCaverns {

	@Shadow @Final private ServerWorld world;

	@Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
	private void bigglobe_overridePortalLocation(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockLocating.Rectangle>> callback) {
		if (this.world.getChunkManager().getChunkGenerator() instanceof BigGlobeNetherChunkGenerator) {
			BigGlobeMod.LOGGER.info("Attempting to find nether portal location at high Y level...");
			Vec3i size = switch (axis) {
				case X -> new Vec3i(4, 5, 1);
				case Z -> new Vec3i(1, 5, 4);
				case Y -> throw new IllegalArgumentException("Y axis");
			};
			BlockPos bestPos = NetherPortalUtil.findBestPortalPosition(this.world, pos, size);
			if (bestPos != null) {
				BlockBox box = NetherPortalUtil.toBoundingBox(bestPos, size);
				BigGlobeMod.LOGGER.info("Found good portal location: " + box);
				BlockPos.Mutable mutablePos = new BlockPos.Mutable();
				for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
					for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
						for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
							this.world.setBlockState(
								mutablePos.set(x, y, z),
								(y != box.getMinY() && y != box.getMaxY()) && (
									(x != box.getMinX() && x != box.getMaxX()) ||
									(z != box.getMinZ() && z != box.getMaxZ())
								)
								? Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis)
								: BlockStates.OBSIDIAN,
								Block.NOTIFY_LISTENERS | Block.FORCE_STATE
							);
						}
					}
				}
				callback.setReturnValue(
					Optional.of(
						new Rectangle(
							switch (axis) {
								case X -> new BlockPos(box.getMinX() + 1, box.getMinY() + 1, box.getMinZ());
								case Z -> new BlockPos(box.getMinX(), box.getMinY() + 1, box.getMinZ() + 1);
								case Y -> throw new AssertionError("Y axis");
							},
							2,
							3
						)
					)
				);
			}
			else {
				BigGlobeMod.LOGGER.error("Unable to find nether portal location at high Y level.");
			}
		}
	}
}