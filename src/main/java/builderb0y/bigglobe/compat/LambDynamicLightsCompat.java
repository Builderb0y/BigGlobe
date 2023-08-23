package builderb0y.bigglobe.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.TorchArrowEntity;

public class LambDynamicLightsCompat implements DynamicLightsInitializer {

	@Override
	public void onInitializeDynamicLights() {
		//for some reason, manifold doesn't like anonymous classes.
		//also I can't rely on DynamicLightHandler.makeHandler()
		//because type T is bounded to LivingEntity.
		class Handler implements DynamicLightHandler<TorchArrowEntity> {

			@Override
			public int getLuminance(TorchArrowEntity entity) {
				return 14;
			}

			@Override
			public boolean isWaterSensitive(TorchArrowEntity lightSource) {
				return true;
			}
		}
		DynamicLightHandlers.registerDynamicLightHandler(BigGlobeEntityTypes.TORCH_ARROW, new Handler());
	}
}