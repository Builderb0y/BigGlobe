package builderb0y.bigglobe.versions;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import org.joml.Matrix4fc;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

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
}