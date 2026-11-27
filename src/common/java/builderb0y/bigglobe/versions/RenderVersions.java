package builderb0y.bigglobe.versions;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

#if MC_VERSION >= MC_26_3_0
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.renderpearl.api.commands.RenderPass;
	import com.mojang.renderpearl.api.pipeline.RenderPipeline;
	import com.mojang.renderpearl.api.textures.GpuSampler;
	import com.mojang.renderpearl.api.textures.GpuTextureView;
#else
	import com.mojang.blaze3d.pipeline.RenderPipeline;
	import com.mojang.blaze3d.pipeline.RenderTarget;
	import com.mojang.blaze3d.systems.RenderPass;
	import com.mojang.blaze3d.systems.RenderSystem;
	import com.mojang.blaze3d.textures.GpuSampler;
	import com.mojang.blaze3d.textures.GpuTextureView;
#endif

@Environment(EnvType.CLIENT)
public class RenderVersions {

	public static float partialTicks(LevelExtractionContext context) {
		return context.deltaTracker().getGameTimeDeltaPartialTick(false);
	}

	public static boolean isTranslucent(RenderType layer) {
		return layer.sortOnUpload();
	}

	public static Vec3 getCameraPosition(Camera camera) {
		return camera.position();
	}

	public static RenderTarget getMainRenderTarget() {
		#if MC_VERSION >= MC_26_2_0
			return Minecraft.getInstance().gameRenderer.mainRenderTarget();
		#else
			return Minecraft.getInstance().getMainRenderTarget();
		#endif
	}

	public static RenderPass createRenderPass(String name, GpuTextureView colortex) {
		return createRenderPass(() -> name, colortex);
	}

	public static RenderPass createRenderPass(Supplier<String> name, GpuTextureView colortex) {
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(
			name,
			colortex,
			#if MC_VERSION >= MC_26_2_0
				Optional.empty()
			#else
				OptionalInt.empty()
			#endif
		);
	}

	public static RenderPass createRenderPass(String name, GpuTextureView colortex, GpuTextureView depthtex) {
		return createRenderPass(() -> name, colortex, depthtex);
	}

	public static RenderPass createRenderPass(Supplier<String> name, GpuTextureView colortex, GpuTextureView depthtex) {
		return RenderSystem.getDevice().createCommandEncoder().createRenderPass(
			name,
			colortex,
			#if MC_VERSION >= MC_26_2_0
				Optional.empty(),
			#else
				OptionalInt.empty(),
			#endif
			depthtex,
			OptionalDouble.empty()
		);
	}

	public static void drawScreenTriangle(RenderPass pass) {
		#if MC_VERSION >= MC_26_2_0
			pass.draw(3, 1, 0, 0);
		#else
			pass.draw(0, 3);
		#endif
	}

	public static void bindSampler(RenderPass pass, String name, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler) {
		#if MC_VERSION >= MC_26_3_0
			pass.setUniform(name, textureView, sampler);
		#else
			pass.bindTexture(name, textureView, sampler);
		#endif
	}

	public static boolean isRenderPipelineValid(RenderPipeline pipeline) {
		#if MC_VERSION >= MC_26_3_0
			return RenderSystem.getCompiledPipelineNullable(pipeline) != null;
		#else
			return RenderSystem.getDevice().precompilePipeline(pipeline).isValid();
		#endif
	}

	public static void setPipeline(RenderPass pass, RenderPipeline pipeline) {
		#if MC_VERSION >= MC_26_3_0
			pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
		#else
			pass.setPipeline(pipeline);
		#endif
	}
}