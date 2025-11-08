package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

import builderb0y.bigglobe.BigGlobeMod;

@Environment(EnvType.CLIENT)
public class BigGlobeEntityRenderers {

	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering entity renderers...");
		#if MC_VERSION >= MC_1_21_9
			net.minecraft.client.render.entity.EntityRendererFactories.register(BigGlobeEntityTypes.TORCH_ARROW,  TorchArrowRenderer::new);
			net.minecraft.client.render.entity.EntityRendererFactories.register(BigGlobeEntityTypes.ROCK,   FlyingItemEntityRenderer::new);
			net.minecraft.client.render.entity.EntityRendererFactories.register(BigGlobeEntityTypes.STRING,     StringEntityRenderer::new);
			net.minecraft.client.render.entity.EntityRendererFactories.register(BigGlobeEntityTypes.WAYPOINT, WaypointEntityRenderer::new);
		#else
			EntityRendererRegistry.register(BigGlobeEntityTypes.TORCH_ARROW,  TorchArrowRenderer::new);
			EntityRendererRegistry.register(BigGlobeEntityTypes.ROCK,   FlyingItemEntityRenderer::new);
			EntityRendererRegistry.register(BigGlobeEntityTypes.STRING,     StringEntityRenderer::new);
			EntityRendererRegistry.register(BigGlobeEntityTypes.WAYPOINT, WaypointEntityRenderer::new);
		#endif
		BigGlobeMod.LOGGER.debug("Done registering entity renderers.");
	}
}