package builderb0y.bigglobe.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.world.entity.Entity;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity;

public class LambDynamicLightsCompat implements DynamicLightsInitializer {

	@Override
	public void onInitializeDynamicLights(

		DynamicLightsContext context

	) {
		try {
			LambDynamicLightsCode.init();
		}
		catch (LinkageError error) {
			BigGlobeMod.LOGGER.error("Failed to setup LambDynamicLights support. Did the API change?", error);
		}
	}

	public static class LambDynamicLightsCode {

		public static void init() {

			enum WaypointEntityLuminance implements EntityLuminance {
				INSTANCE;

				public static final Type TYPE = Type.registerSimple(BigGlobeMod.modID("waypoint"), INSTANCE);

				@Override
				public Type type() {
					return TYPE;
				}

				@Override
				public int getLuminance(ItemLightSourceManager manager, Entity entity) {
					return entity instanceof WaypointEntity waypoint ? ((int)(waypoint.health * (15.0F / WaypointEntity.MAX_HEALTH))) : 0;
				}
			}
			WaypointEntityLuminance.INSTANCE.getClass(); //force initialize.
		}
	}
}