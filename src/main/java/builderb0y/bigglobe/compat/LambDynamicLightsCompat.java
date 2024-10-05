package builderb0y.bigglobe.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.TorchArrowEntity;
import builderb0y.bigglobe.entities.WaypointEntity;

public class LambDynamicLightsCompat implements DynamicLightsInitializer {

	@Override
	public void onInitializeDynamicLights() {
		try {
			LambDynamicLightsCode.init();
		}
		catch (LinkageError error) {
			BigGlobeMod.LOGGER.error("Failed to setup LambDynamicLights support. Did the API change?", error);
		}
	}

	public static class LambDynamicLightsCode {

		public static void init() {
			//for some reason, manifold doesn't like this one specific anonymous class,
			//so I made it a local class instead.
			//also I can't rely on DynamicLightHandler.makeHandler()
			//because type T is bounded to LivingEntity.
			class TorchArrowHandler implements DynamicLightHandler<TorchArrowEntity> {

				@Override
				public int getLuminance(TorchArrowEntity entity) {
					return 14;
				}

				@Override
				public boolean isWaterSensitive(TorchArrowEntity lightSource) {
					return true;
				}
			}
			DynamicLightHandlers.registerDynamicLightHandler(BigGlobeEntityTypes.TORCH_ARROW, new TorchArrowHandler());

			class WaypointHandler implements DynamicLightHandler<WaypointEntity> {

				@Override
				public int getLuminance(WaypointEntity waypoint) {
					return ((int)(waypoint.health * (15.0F / WaypointEntity.MAX_HEALTH)));
				}
			}
			DynamicLightHandlers.registerDynamicLightHandler(BigGlobeEntityTypes.WAYPOINT, new WaypointHandler());
		}
	}
}