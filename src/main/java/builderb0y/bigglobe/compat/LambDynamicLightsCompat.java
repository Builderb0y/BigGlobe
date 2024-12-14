package builderb0y.bigglobe.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import net.minecraft.entity.Entity;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.TorchArrowEntity;
import builderb0y.bigglobe.entities.WaypointEntity;

#if MC_VERSION < MC_1_21_4
	import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
	import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
#endif

public class LambDynamicLightsCompat implements DynamicLightsInitializer {

	@Override
	public void onInitializeDynamicLights(
		#if MC_VERSION >= MC_1_21_4
			dev.lambdaurora.lambdynlights.api.DynamicLightsContext context
		#elif MC_VERSION >= MC_1_21_0
			dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager manager
		#endif
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
			#if MC_VERSION >= MC_1_21_4
				enum WaypointEntityLuminance implements dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance {
					INSTANCE;

					public static final Type TYPE = Type.registerSimple(BigGlobeMod.modID("waypoint"), INSTANCE);

					@Override
					public @NotNull Type type() {
						return TYPE;
					}

					@Override
					public @Range(from = 0L, to = 15L) int getLuminance(@NotNull ItemLightSourceManager manager, @NotNull Entity entity) {
						return entity instanceof WaypointEntity waypoint ? ((int)(waypoint.health * (15.0F / WaypointEntity.MAX_HEALTH))) : 0;
					}
				}
				WaypointEntityLuminance.INSTANCE.getClass(); //force initialize.
			#else
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
			#endif
		}
	}
}