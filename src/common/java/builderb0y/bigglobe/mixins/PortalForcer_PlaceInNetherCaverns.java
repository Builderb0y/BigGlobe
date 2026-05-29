package builderb0y.bigglobe.mixins;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.BlockUtil.FoundRectangle;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.portal.PortalForcer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.util.NetherPortalUtil;

/**
vanilla logic likes to place you on the surface of the lava ocean,
which is bad for gameplay. I'm making it so that you spawn in the cavern area, near the top.
*/
@Mixin(PortalForcer.class)
public class PortalForcer_PlaceInNetherCaverns {

	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
	private void bigglobe_overridePortalLocation(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> callback) {
		if (
			this.level.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.game_mechanics.nether() != null &&
			generator.game_mechanics.nether().place_portal_at_high_y_level()
		) {
			BigGlobeMod.LOGGER.info("Attempting to find nether portal location at high Y level...");
			Vec3i size = switch (axis) {
				case X -> new Vec3i(4, 5, 1);
				case Z -> new Vec3i(1, 5, 4);
				case Y -> throw new IllegalArgumentException("Y axis");
			};
			BlockPos bestPos = NetherPortalUtil.findBestPortalPosition(this.level, pos, size);
			if (bestPos != null) {
				BoundingBox box = NetherPortalUtil.toBoundingBox(bestPos, size);
				BigGlobeMod.LOGGER.info("Found good portal location: " + box);
				BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
				BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
				for (int z = box.minZ(); z <= box.maxZ(); z++) {
					for (int x = box.minX(); x <= box.maxX(); x++) {
						for (int y = box.minY(); y <= box.maxY(); y++) {
							this.level.setBlock(
								mutablePos.set(x, y, z),
								(y != box.minY() && y != box.maxY()) && (
									(x != box.minX() && x != box.maxX()) ||
									(z != box.minZ() && z != box.maxZ())
								)
									? portalState
									: BlockStates.OBSIDIAN,
								Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
							);
						}
					}
				}
				callback.setReturnValue(
					Optional.of(
						new FoundRectangle(
							switch (axis) {
								case X -> new BlockPos(box.minX() + 1, box.minY() + 1, box.minZ());
								case Z -> new BlockPos(box.minX(), box.minY() + 1, box.minZ() + 1);
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