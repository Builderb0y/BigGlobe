package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalMatcher;
import qouteall.q_misc_util.my_util.IntBox;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.util.NetherPortalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(NetherPortalMatcher.class)
public class ImmersivePortals_NetherPortalMatcher_PlacePortalHigherInBigGlobeWorlds {

	@Inject(method = "findVerticalPortalPlacement", at = @At("HEAD"), cancellable = true, remap = false)
	private static void bigglobe_movePortal(
		BlockPos areaSize,
		LevelAccessor world,
		BlockPos searchingCenter,
		CallbackInfoReturnable<IntBox> callback
	) {
		if (
			world.getChunkSource() instanceof ServerChunkCache serverChunkManager &&
			serverChunkManager.getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.nether_overrides != null &&
			generator.nether_overrides.place_portal_at_high_y_level()
		) {
			BlockPos bestPosition = NetherPortalUtil.findBestPortalPosition(world, searchingCenter, areaSize);
			if (bestPosition != null) {
				BoundingBox box = NetherPortalUtil.toBoundingBox(bestPosition, areaSize);
				BigGlobeMod.LOGGER.info("Overriding immersive portal position: " + box);
				callback.setReturnValue(
					new IntBox(
						new BlockPos(box.minX(), box.minY(), box.minZ()),
						new BlockPos(box.maxX(), box.maxY(), box.maxZ())
					)
				);
			}
			else {
				BigGlobeMod.LOGGER.error("Could not find suitable nether portal location.");
			}
		}
	}
}