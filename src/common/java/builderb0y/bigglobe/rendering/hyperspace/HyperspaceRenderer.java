package builderb0y.bigglobe.rendering.hyperspace;

import java.util.Random;

import com.mojang.renderpearl.api.pipeline.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.util.ClientWorldEvents;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public abstract class HyperspaceRenderer implements SafeCloseable {

	public static final RenderPipeline.Snippet
		WITHOUT_PREVIOUS = (
			RenderPipeline
			.builder()
			.withVertexShader(BigGlobeMod.mcID("core/screenquad"))
			#if MC_VERSION >= MC_26_2_0
				.withBindGroupLayout(
					BindGroupLayout
					.builder()
					.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
					.build()
				)
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				#if MC_VERSION >= MC_26_3_0
					.withColorTargetState(ColorTargetState.DEFAULT)
				#endif
			#else
				.withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
				.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			#endif
			.buildSnippet()
		),
		WITH_PREVIOUS = (
			RenderPipeline
			.builder(WITHOUT_PREVIOUS)
			#if MC_VERSION >= MC_26_2_0
				.withBindGroupLayout(
					BindGroupLayout
					.builder()
					#if MC_VERSION >= MC_26_3_0
						.withUniform("previousPass", UniformType.COMBINED_IMAGE_SAMPLER)
					#else
						.withSampler("previousPass")
					#endif
					.build()
				)
			#else
				.withSampler("previousPass")
			#endif
			.buildSnippet()
		);

	public static HyperspaceRenderer INSTANCE;
	static {
		//the instance can become null again if it encounters an exception while rendering.
		//and since you can't un-register an event handler, it needs to check for null instead.
		ClientTickEvents.START_LEVEL_TICK.register((ClientLevel level) -> {
			if (INSTANCE != null && level.dimension() == HyperspaceConstants.WORLD_KEY) INSTANCE.tick();
		});
		#if MC_VERSION >= MC_26_2_0
			net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents
		#else
			LevelRenderEvents
		#endif
		.END_EXTRACTION.register((LevelExtractionContext context) -> {
			if (INSTANCE != null) INSTANCE.extract(context);
		});
		LevelRenderEvents.#if MC_VERSION >= MC_26_3_0 BEFORE_RENDER_PASS #else START_MAIN #endif.register((LevelTerrainRenderContext context) -> {
			if (INSTANCE != null) INSTANCE.render(context);
		});
		InvalidateRenderStateCallback.EVENT.register(HyperspaceRenderer::init);
		ClientWorldEvents.WORLD_CHANGED.register((ClientLevel oldWorld, ClientLevel newWorld) -> {
			init(newWorld);
		});
	}

	public static void init() {
		init(Minecraft.getInstance().level);
	}

	public static void init(ClientLevel world) {
		try {
			HyperspaceRenderer old = INSTANCE;
			if (old != null) {
				INSTANCE = null;
				old.close();
			}
			if (world != null && world.dimension() == HyperspaceConstants.WORLD_KEY) {
				INSTANCE = HyperspaceRenderers.getRandom();
			}
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.warn("Fancy hyperspace background unavailable:", exception);
		}
	}

	public float collapse;
	public boolean closed;

	@Override
	public void close() {
		this.closed = true;
	}

	public abstract void tick();

	public void extract(LevelExtractionContext context) {
		float collapse = 0.0F;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			PlayerWaypointManager manager = PlayerWaypointManager.get(player);
			if (manager != null) {
				collapse = manager.collapseProgress + (
					manager.getAllWaypoints().isEmpty()
					? +context.deltaTracker().getGameTimeDeltaPartialTick(false)
					: -context.deltaTracker().getGameTimeDeltaPartialTick(false)
				);
			}
		}
		this.collapse = collapse / PlayerWaypointManager.COLLAPSE_DURATION_TICKS;
	}

	public void render(LevelTerrainRenderContext context) {
		if (this.closed) {
			BigGlobeMod.LOGGER.error("Attempt to render closed hyperspace renderer");
			INSTANCE = null;
			return;
		}
		try {
			this.doRender(context);
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("Exception rendering hyperspace background. The fancy hyperspace background will now be disabled to prevent more issues later.", exception);
			INSTANCE = null;
			this.close();
		}
	}

	public abstract void doRender(LevelTerrainRenderContext context);

	public static interface Factory {

		public abstract HyperspaceRenderer create();
	}
}