package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity.Orbit;
import builderb0y.bigglobe.math.BigGlobeMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Environment(EnvType.CLIENT)
public class WaypointEntityRenderer extends BigGlobeEntityRenderer<WaypointEntity, WaypointEntityRenderer.State> {

	public static RenderType LIGHTNING_LAYER;

	static {
		RenderType layer = RenderTypes.lightning();
		got:
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			try {
				layer = (RenderType)(Class.forName("net.irisshaders.iris.pathways.LightningHandler").getDeclaredField("IRIS_LIGHTNING").get(null));
				BigGlobeMod.LOGGER.info("Using new iris lightning render layer.");
				break got;
			}
			catch (Exception ignored) {
			}
			try {
				layer = (RenderType)(Class.forName("net.coderbot.iris.pipeline.LightningHandler").getDeclaredField("IRIS_LIGHTNING").get(null));
				BigGlobeMod.LOGGER.info("Using old iris lightning render layer.");
				break got;
			}
			catch (Exception ignored) {
			}
			BigGlobeMod.LOGGER.info("Could not locate iris lightning render layer...");
		}
		LIGHTNING_LAYER = layer;
	}

	public WaypointEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public RenderType getRenderLayer() {
		return LIGHTNING_LAYER;
	}

	@Override
	public void doRender(WaypointEntityRenderer.State state, PoseStack.Pose matrices, VertexConsumer buffer, Vec3 camera, int light) {
		Vector3f cameraToPos = new Vector3f(
			(float)(state.x - camera.x),
			(float)(state.y - camera.y + 1.0D),
			(float)(state.z - camera.z)
		);
		Vector4f
			transform = new Vector4f();
		Vector3f
			position = new Vector3f(),
			tangent = new Vector3f(),
			side = new Vector3f(),
			prevPosition = new Vector3f(),
			prevTangent = new Vector3f(),
			prevSide = new Vector3f();
		float
			size = 0.0F,
			prevSize = 0.0F;
		int maxOrbits = BigGlobeMath.roundI(state.health / WaypointEntity.MAX_HEALTH * state.orbits.length);
		for (int orbitIndex = 0; orbitIndex < maxOrbits; orbitIndex++) {
			Orbit orbit = state.orbits[orbitIndex];
			for (int history = 0; history <= 32; history++) {
				if (history != 0) {
					prevPosition.set(position);
					prevTangent.set(tangent);
					prevSide.set(side);
					prevSize = size;
				}
				float historyF = history * 0.03125F;
				orbit.getPositionAndVelocity(position, tangent, historyF * historyF, state.partialTicks);
				position.add(cameraToPos).cross(tangent, side).normalize();
				size = ((float)(Math.sqrt(historyF))) * BigGlobeMath.squareF(1.0F - historyF) * 0.25F;
				if (history != 0) {
					vertex(
						buffer,
						matrices,
						prevPosition.x,
						prevPosition.y,
						prevPosition.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						prevPosition.x + prevSide.x * prevSize,
						prevPosition.y + prevSide.y * prevSize,
						prevPosition.z + prevSide.z * prevSize,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x + side.x * size,
						position.y + side.y * size,
						position.z + side.z * size,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x,
						position.y,
						position.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);

					vertex(
						buffer,
						matrices,
						prevPosition.x - prevSide.x * prevSize,
						prevPosition.y - prevSide.y * prevSize,
						prevPosition.z - prevSide.z * prevSize,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						prevPosition.x,
						prevPosition.y,
						prevPosition.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x,
						position.y,
						position.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x - side.x * size,
						position.y - side.y * size,
						position.z - side.z * size,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
				}
			}
		}
	}

	public static void vertex(
		VertexConsumer buffer,
		PoseStack.Pose matrices,
		float x,
		float y,
		float z,
		float r,
		float g,
		float b,
		float a,
		Vector4f scratch
	) {
		buffer.addVertex(x, y, z).setColor(r, g, b, a);
	}

	@Override
	public WaypointEntityRenderer.State createState() {
		return new WaypointEntityRenderer.State();
	}

	@Override
	public void updateState(WaypointEntity entity, WaypointEntityRenderer.State state, float partialTicks) {
		state.ageInTicks = entity.tickCount + partialTicks;
		state.partialTicks = partialTicks;
		state.health = entity.health;
		state.orbits = Orbit.copy(entity.orbits, state.orbits);
	}

	public static class State extends BigGlobeEntityRenderer.State {

		public float health, partialTicks;
		public WaypointEntity.Orbit[] orbits;
	}
}