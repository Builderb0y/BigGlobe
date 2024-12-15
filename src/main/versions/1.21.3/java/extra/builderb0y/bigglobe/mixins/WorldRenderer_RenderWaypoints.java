package builderb0y.bigglobe.mixins;

import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.entity.Entity;

import builderb0y.bigglobe.entities.WaypointEntity;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering;
import builderb0y.bigglobe.hyperspace.VanillaHyperspaceRendering;

@Mixin(WorldRenderer.class)
public class WorldRenderer_RenderWaypoints {

	@Shadow @Final private DefaultFramebufferSet framebufferSet;

	@Shadow @Final private List<Entity> renderedEntities;

	@Inject(
		method = "render",
		at = @At(
			value  = "INVOKE",
			target = "Lnet/minecraft/client/render/WorldRenderer;renderLateDebug(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/Fog;)V",
			shift  = Shift.AFTER
		)
	)
	private void bigglobe_renderWaypoints(
		ObjectAllocator allocator,
		RenderTickCounter tickCounter,
		boolean renderBlockOutline,
		Camera camera,
		GameRenderer gameRenderer,
		LightmapTextureManager lightmapTextureManager,
		Matrix4f positionMatrix,
		Matrix4f projectionMatrix,
		CallbackInfo callback,
		@Local FrameGraphBuilder frameGraphBuilder
	) {
		for (Entity entity : this.renderedEntities) {
			if (entity instanceof WaypointEntity waypoint) {
				HyperspaceRendering.markWaypointVisible(waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.age, waypoint.health);
			}
		}
		VanillaHyperspaceRendering.renderWaypoints(frameGraphBuilder, tickCounter.getTickDelta(false), this.framebufferSet);
	}
}