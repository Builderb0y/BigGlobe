package builderb0y.bigglobe.rendering2.lods;

import java.nio.ByteOrder;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import org.lwjgl.system.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;

import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.rendering2.NativeMemory;
import builderb0y.bigglobe.util.SafeCloseable;

public abstract class LodRenderer implements SafeCloseable {

	public static final int
		MATRICES_SIZE   = 64,
		FOG_BUFFER_SIZE = 16;

	public final LodFrustum frustum;
	public final GpuBuffer fogGpuBuffer, matricesGpuBuffer;
	public final NativeMemory fogCpuBuffer;
	public float rain, thunder;

	public LodRenderer(LodSystem system) {
		this.frustum = new LodFrustum(system);
		this.fogGpuBuffer = RenderSystem.getDevice().createBuffer(
			() -> "Big Globe LodRenderer.fogGpuBuffer",
			GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
			FOG_BUFFER_SIZE
		);
		this.matricesGpuBuffer = RenderSystem.getDevice().createBuffer(
			() -> "Big Globe LodRenderer.matricesGpuBuffer",
			GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
			MATRICES_SIZE
		);
		this.fogCpuBuffer = new NativeMemory(FOG_BUFFER_SIZE);
	}

	public abstract LodSystem getSystem();

	public void extract(LevelExtractionContext context) {
		this.frustum.setup(context);
		this.rain = context.level().getRainLevel(context.deltaTracker().getGameTimeDeltaPartialTick(false));
		this.thunder = context.level().getThunderLevel(context.deltaTracker().getGameTimeDeltaPartialTick(false));
	}

	public void uploadBuffers() {
		this.uploadFog();
		this.uploadMatrices();
	}

	public void uploadFog() {
		float globalFogDensity = BigGlobeConfig.INSTANCE.get().lodRendering.fogDensity;
		float fogHeightScale = BigGlobeConfig.INSTANCE.get().lodRendering.fogHeightScale;
		ClientGeneratorParams params = this.getSystem().params;
		Number baseHeight;
		globalFogDensity *= params.generatorLodOverrides.fog_density_multiplier();
		Float scale = params.generatorLodOverrides.fog_height_scale();
		if (scale != null) fogHeightScale = scale;
		baseHeight = params.generatorLodOverrides.fog_base_height();
		if (baseHeight == null) baseHeight = params.seaLevel;

		if (baseHeight != null && fogHeightScale != 0.0F) {
			this
			.fogCpuBuffer
			.appendFloat((float)(this.frustum.y - baseHeight.doubleValue()), ByteOrder.nativeOrder())
			.appendFloat(fogHeightScale / ((float)(params.maxY - baseHeight.doubleValue())), ByteOrder.nativeOrder())
			.appendFloat(
				Interpolator.mixSmoothUnchecked(
					1.0F,
					Interpolator.mixSmoothUnchecked(2.0F, 4.0F, this.thunder),
					this.rain
				)
				* globalFogDensity / this.frustum.farClippingPlane,
				ByteOrder.nativeOrder()
			);
		}
		else {
			this
			.fogCpuBuffer
			.appendFloat(0.0F, ByteOrder.nativeOrder())
			.appendFloat(0.0F, ByteOrder.nativeOrder())
			.appendFloat(globalFogDensity / this.frustum.farClippingPlane, ByteOrder.nativeOrder());
		}
		RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.fogGpuBuffer.slice(), this.fogCpuBuffer.toByteBuffer());
		this.fogCpuBuffer.clear();
	}

	public void uploadMatrices() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
				this.matricesGpuBuffer.slice(),
				Std140Builder.onStack(stack, MATRICES_SIZE).putMat4f(this.getSystem().getRenderer().frustum.modelViewProjectionMatrix).get()
			);
		}
	}

	public void beginRendering(RenderPass pass) {
		pass.setUniform("Fog", RenderSystem.getShaderFog());
		pass.setUniform("ExtraFog", this.fogGpuBuffer);
		pass.setUniform("Matrices", this.matricesGpuBuffer);
		pass.bindTexture("blockAtlas", Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true));
		pass.bindTexture("lightmap", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
	}

	public abstract void render(RenderPass pass, ChunkSectionLayer layer);

	public abstract QuadPacker<?> createPacker();
}