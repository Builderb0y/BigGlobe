package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalMatcher;
import qouteall.q_misc_util.my_util.IntBox;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.util.NetherPortalUtil;

@Mixin(NetherPortalMatcher.class)
public class ImmersivePortals_NetherPortalMatcher_PlacePortalHigherInBigGlobeWorlds {

	@Inject(method = "findVerticalPortalPlacement", at = @At("HEAD"), cancellable = true, remap = false)
	private static void bigglobe_movePortal(
		BlockPos areaSize,
		WorldAccess world,
		BlockPos searchingCenter,
		CallbackInfoReturnable<IntBox> callback
	) {
		if (
			world.getChunkManager() instanceof ServerChunkManager serverChunkManager &&
			serverChunkManager.getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.nether_overrides != null &&
			generator.nether_overrides.place_portal_at_high_y_level()
		) {
			BlockPos bestPosition = NetherPortalUtil.findBestPortalPosition(world, searchingCenter, areaSize);
			if (bestPosition != null) {
				BlockBox box = NetherPortalUtil.toBoundingBox(bestPosition, areaSize);
				BigGlobeMod.LOGGER.info("Overriding immersive portal position: " + box);
				callback.setReturnValue(
					new IntBox(
						new BlockPos(box.getMinX(), box.getMinY(), box.getMinZ()),
						new BlockPos(box.getMaxX(), box.getMaxY(), box.getMaxZ())
					)
				);
			}
			else {
				BigGlobeMod.LOGGER.error("Could not find suitable nether portal location.");
			}
		}
	}
}