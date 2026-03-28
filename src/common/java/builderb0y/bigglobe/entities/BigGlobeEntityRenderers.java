package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import builderb0y.bigglobe.BigGlobeMod;

@Environment(EnvType.CLIENT)
public class BigGlobeEntityRenderers {

	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering entity renderers...");

		EntityRenderers.register(BigGlobeEntityTypes.TORCH_ARROW, TorchArrowRenderer::new);
		EntityRenderers.register(BigGlobeEntityTypes.ROCK, ThrownItemRenderer::new);
		EntityRenderers.register(BigGlobeEntityTypes.STRING, StringEntityRenderer::new);
		EntityRenderers.register(BigGlobeEntityTypes.WAYPOINT, WaypointEntityRenderer::new);

		BigGlobeMod.LOGGER.debug("Done registering entity renderers.");
	}
}