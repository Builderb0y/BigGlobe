package builderb0y.bigglobe.mixins;

import java.util.List;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;

import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering;
import builderb0y.bigglobe.rendering.waypoints.WaypointWarpRenderer;

@Mixin(WorldRenderer.class)
public class WorldRenderer_RenderWaypoints {

	#if MC_VERSION >= MC_1_21_3

		@Shadow @Final private List<Entity> renderedEntities;

		@Inject(method = "method_62214", at = @At("RETURN"))
		private void bigglobe_renderWaypoints(
			#if MC_VERSION >= MC_1_21_6
				com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
				RenderTickCounter renderTickCounter,
				Camera camera,
				Profiler profiler,
				Matrix4f matrix4f,
				net.minecraft.client.util.Handle handle,
				net.minecraft.client.util.Handle handle2,
				boolean bl,
				Frustum frustum,
				net.minecraft.client.util.Handle handle3,
				net.minecraft.client.util.Handle handle4,
				CallbackInfo callback
			#elif MC_VERSION >= MC_1_21_5
				Fog fog,
				RenderTickCounter renderTickCounter,
				Camera camera,
				Profiler profiler,
				Matrix4f matrix4f,
				Matrix4f matrix4f2,
				net.minecraft.client.util.Handle handle,
				net.minecraft.client.util.Handle handle2,
				boolean bl,
				Frustum frustum,
				net.minecraft.client.util.Handle handle3,
				net.minecraft.client.util.Handle handle4,
				CallbackInfo callback
			#else
				Fog fog,
				RenderTickCounter renderTickCounter,
				Camera camera,
				Profiler profiler,
				Matrix4f matrix4f,
				Matrix4f matrix4f2,
				net.minecraft.client.util.Handle handle,
				net.minecraft.client.util.Handle handle2,
				net.minecraft.client.util.Handle handle3,
				net.minecraft.client.util.Handle handle4,
				boolean bl,
				Frustum frustum,
				net.minecraft.client.util.Handle handle5,
				CallbackInfo callback
			#endif
		) {
			this.bigglobe_renderWaypoints();
		}

	#else

		@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDD)V"))
		private void bigglobe_renderWaypoints(
			#if MC_VERSION >= MC_1_21_0
				RenderTickCounter tickCounter,
				boolean renderBlockOutline,
				Camera camera,
				GameRenderer gameRenderer,
				LightmapTextureManager lightmapTextureManager,
				Matrix4f matrix4f,
				Matrix4f matrix4f2,
				CallbackInfo callback
			#elif MC_VERSION >= MC_1_20_5
				float tickDelta,
				long limitTime,
				boolean renderBlockOutline,
				Camera camera,
				GameRenderer gameRenderer,
				LightmapTextureManager lightmapTextureManager,
				Matrix4f matrix4f,
				Matrix4f matrix4f2,
				CallbackInfo callback
			#else
				MatrixStack matrices,
				float tickDelta,
				long limitTime,
				boolean renderBlockOutline,
				Camera camera,
				GameRenderer gameRenderer,
				LightmapTextureManager lightmapTextureManager,
				Matrix4f projectionMatrix,
				CallbackInfo callback
			#endif
		) {
			this.bigglobe_renderWaypoints();
		}

	#endif

	@Unique
	private void bigglobe_renderWaypoints() {
		if (WaypointWarpRenderer.INSTANCE != null) {
			#if MC_VERSION > MC_1_21_1
				for (Entity entity : this.renderedEntities) {
					if (entity instanceof WaypointEntity waypoint) {
						HyperspaceRendering.markWaypointVisible(waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.age, waypoint.health);
					}
				}
			#endif
			if (!HyperspaceRendering.visibleWaypoints.isEmpty()) {
				WaypointWarpRenderer.INSTANCE.draw();
			}
		}
	}
}