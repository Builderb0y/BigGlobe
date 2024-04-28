package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.satin.SatinCompat;
import builderb0y.bigglobe.entities.WaypointEntity.Orbit;
import builderb0y.bigglobe.math.BigGlobeMath;

@Environment(EnvType.CLIENT)
public class WaypointEntityRenderer extends EntityRenderer<WaypointEntity> {

	public static final Identifier TEXTURE = BigGlobeMod.mcID("textures/particle/flash.png");

	public WaypointEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(WaypointEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		int fullbright = LightmapTextureManager.pack(15, LightmapTextureManager.getSkyLightCoordinates(light));
		Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().subtract(entity.getPos());
		Vector3f normal = new Vector3f().set(camera.x, camera.y, camera.z).normalize();
		Vector3f unit1 = new Vector3f(normal).cross(0.0F, 1.0F, 0.0F).normalize();
		Vector3f unit2 = new Vector3f(unit1).cross(normal).normalize();
		Vector3f scratch = new Vector3f();
		int maxOrbits = BigGlobeMath.roundI(entity.health / WaypointEntity.MAX_HEALTH * entity.orbits.length);
		for (int i = 0; i < maxOrbits; i++) {
			Orbit orbit = entity.orbits[i];
			for (int history = 0; history < 16; history++) {
				orbit.getPosition(scratch, history);
				float size = history * (-1.0F / 16.0F / 16.0F) + 0.0625F;

				buffer
				.vertex(
					matrices.peek().getPositionMatrix(),
					scratch.x + (unit1.x + unit2.x) * size,
					scratch.y + (unit1.y + unit2.y) * size + 1.0F,
					scratch.z + (unit1.z + unit2.z) * size
				)
				.texture(0.0F, 0.0F)
				.normal(matrices.peek().getNormalMatrix(), 0.0F, 1.0F, 0.0F)
				.color(orbit.color)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(fullbright)
				.next();

				buffer
				.vertex(
					matrices.peek().getPositionMatrix(),
					scratch.x + (unit1.x - unit2.x) * size,
					scratch.y + (unit1.y - unit2.y) * size + 1.0F,
					scratch.z + (unit1.z - unit2.z) * size
				)
				.texture(0.0F, 1.0F)
				.normal(matrices.peek().getNormalMatrix(), 0.0F, 1.0F, 0.0F)
				.color(orbit.color)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(fullbright)
				.next();

				buffer
				.vertex(
					matrices.peek().getPositionMatrix(),
					scratch.x + (-unit1.x - unit2.x) * size,
					scratch.y + (-unit1.y - unit2.y) * size + 1.0F,
					scratch.z + (-unit1.z - unit2.z) * size
				)
				.texture(1.0F, 1.0F)
				.normal(matrices.peek().getNormalMatrix(), 0.0F, 1.0F, 0.0F)
				.color(orbit.color)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(fullbright)
				.next();

				buffer
				.vertex(
					matrices.peek().getPositionMatrix(),
					scratch.x + (-unit1.x + unit2.x) * size,
					scratch.y + (-unit1.y + unit2.y) * size + 1.0F,
					scratch.z + (-unit1.z + unit2.z) * size
				)
				.texture(1.0F, 0.0F)
				.normal(matrices.peek().getNormalMatrix(), 0.0F, 1.0F, 0.0F)
				.color(orbit.color)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(fullbright)
				.next();
			}
		}
		SatinCompat.markWaypointRendered(entity);
	}

	@Override
	public Identifier getTexture(WaypointEntity entity) {
		return TEXTURE;
	}
}