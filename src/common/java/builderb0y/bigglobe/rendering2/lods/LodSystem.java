package builderb0y.bigglobe.rendering2.lods;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator.GameMechanics.LodOverrides;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering2.lods.flat.FlatLodSystem;
import builderb0y.bigglobe.rendering2.lods.flat.LodQuadNode;
import builderb0y.bigglobe.util.SafeCloseable;

public abstract class LodSystem implements SafeCloseable {

	static {
		LevelRenderEvents.END_EXTRACTION.register((LevelExtractionContext context) -> {
			LodSystemHolder holder = LodSystemHolder.of(context.levelRenderer());
			LodSystem system = holder.bigglobe_getLodSystem();
			if (system != null) {
				if (system.closed.get()) {
					holder.bigglobe_setLodSystem(null);
				}
				else {
					system.extract(context);
				}
			}
		});
		LevelRenderEvents.START_MAIN.register((LevelTerrainRenderContext context) -> {
			LodSystemHolder holder = LodSystemHolder.of(context.levelRenderer());
			LodSystem system = holder.bigglobe_getLodSystem();
			if (system != null) {
				if (system.closed.get()) {
					holder.bigglobe_setLodSystem(null);
				}
				else {
					system.draw();
				}
			}
		});
	}

	public static void init() {}

	public final ClientGeneratorParams params;
	public final LodOverrides overrides;
	public double currentQuality, qualityLimit, loadDistance;
	public int levelLimit;
	public boolean renderingThisFrame;
	public final AtomicBoolean closed = new AtomicBoolean(false);

	public LodSystem(ClientGeneratorParams params) {
		this.params = params;
		this.overrides = params.generatorLodOverrides;
		this.qualityLimit = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
	}

	public static boolean isAvailable() {
		return LodVertexFormat.FORMAT != null;
	}

	public static void reload(LodSystemHolder holder, ClientLevel world) {
		ClientState state = ClientState.get(world);
		reload(holder, world, state != null ? state.generatorParams : null);
	}

	public static void reload(LodSystemHolder holder, ClientLevel level, ClientGeneratorParams params) {
		if (!isAvailable()) return;
		LodSystem system = holder.bigglobe_getLodSystem();
		if (system != null) {
			system.close();
			holder.bigglobe_setLodSystem(null);
		}
		if (
			level != null &&
			params != null &&
			params.generatorLodOverrides.lod_rendering_enabled() &&
			BigGlobeConfig.INSTANCE.get().lodRendering.renderingEnabled()
		) {
			try {
				holder.bigglobe_setLodSystem(new FlatLodSystem(params, level));
			}
			catch (Exception exception) {
				BigGlobeMod.LOGGER.error("Failed to setup LOD renderer:", exception);
			}
		}
	}

	public String f3Message() {
		double qualityConfig = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
		return "[BG] LOD Quality: " + this.currentQuality + "/" + this.qualityLimit + "/" + qualityConfig + ", L: " + this.levelLimit;
	}

	public void extract(LevelExtractionContext context) {
		this.getRenderer().extract(context);
		this.renderingThisFrame = !(
			context.camera().entity() instanceof LivingEntity livingEntity && (
				livingEntity.hasEffect(MobEffects.BLINDNESS) ||
				livingEntity.hasEffect(MobEffects.DARKNESS)
			)
		);
		this.getTree().updateTree();
		this.getGenerationPipeline().generator.processDirtyChunks();
		this.updateQuality();
	}

	public void updateQuality() {
		LodNode atPlayer = this.getTree().getNodeAtPlayerForDowngradeChecking();
		if (atPlayer != null) {
			if (atPlayer.level > this.levelLimit) {
				this.currentQuality = this.qualityLimit - atPlayer.level;
			}
			else if (this.getGenerationPipeline().requests.isEmpty()) {
				this.currentQuality = Math.min(this.currentQuality + 0.0625D, this.qualityLimit);
				if (this.currentQuality == this.qualityLimit) {
					this.loadDistance = Math.min(this.loadDistance + 16.0D, this.getRenderer().frustum.generationBuffer);
				}
			}
			this.levelLimit = Math.max(atPlayer.level - 1, 0);
		}
		else {
			if (this.getGenerationPipeline().requests.isEmpty()) {
				if (this.levelLimit > 0) {
					this.levelLimit--;
				}
				else {
					this.currentQuality = Math.min(this.currentQuality + 0.0625D, this.qualityLimit);
					if (this.currentQuality == this.qualityLimit) {
						this.loadDistance = Math.min(this.loadDistance + 16.0D, this.getRenderer().frustum.generationBuffer);
					}
				}
			}
		}
	}

	public void draw() {
		if (!this.getGenerationPipeline().isAlive()) {
			BigGlobeMod.LOGGER.error("LOD system shutting down due to generation pipeline failure. It can be restarted with F3+A.");
			this.close();
			return;
		}
		try {
			this.getGenerationPipeline().processSupply();
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("LOD system shutting down due to exception processing LOD mesh supply. It can be restarted with F3+A.", exception);
			this.close();
			return;
		}
		try {
			this.getRenderer().uploadBuffers();
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("LOD system shutting down due to exception uploading LOD buffers. It can be restarted with F3+A.", exception);
			this.close();
			return;
		}
		try {
			try (
				RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "Big Globe LODs",
					Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
					OptionalInt.empty(),
					Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
					OptionalDouble.empty()
				)
			) {
				this.getRenderer().beginRendering(pass);
				for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
					pass.pushDebugGroup(layer::toString);
					this.getRenderer().render(pass, layer);
					pass.popDebugGroup();
				}
			}
			RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(
				Minecraft.getInstance().getMainRenderTarget().getDepthTexture(),
				1.0D
			);
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("LOD system shutting down due to exception rendering LODs. It can be restarted with F3+A.", exception);
			this.close();
			return;
		}
	}

	public abstract LodTree getTree();

	public abstract GenerationPipeline getGenerationPipeline();

	public abstract LodRenderer getRenderer();

	@Override
	public void close() {
		if (this.closed.compareAndSet(false, true)) {
			this.doClose();
		}
	}

	public abstract void doClose();
}