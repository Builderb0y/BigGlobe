package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Vector3f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity.Orbit;
import builderb0y.bigglobe.math.BigGlobeMath;

@Environment(EnvType.CLIENT)
public class WaypointEntityRenderer extends BigGlobeEntityRenderer<WaypointEntity, WaypointEntityRenderer.State> {

	public static final Identifier TEXTURE = BigGlobeMod.mcID("textures/particle/flash.png");
	public static RenderLayer LIGHTNING_LAYER;
	static {
		RenderLayer layer = RenderLayer.getLightning();
		got:
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			try {
				layer = (RenderLayer)(Class.forName("net.irisshaders.iris.pathways.LightningHandler").getDeclaredField("IRIS_LIGHTNING").get(null));
				BigGlobeMod.LOGGER.info("Using new iris lightning render layer.");
				break got;
			}
			catch (Exception ignored) {}
			try {
				layer = (RenderLayer)(Class.forName("net.coderbot.iris.pipeline.LightningHandler").getDeclaredField("IRIS_LIGHTNING").get(null));
				BigGlobeMod.LOGGER.info("Using old iris lightning render layer.");
				break got;
			}
			catch (Exception ignored) {}
			BigGlobeMod.LOGGER.info("Could not locate iris lightning render layer...");
		}
		LIGHTNING_LAYER = layer;
	}

	public WaypointEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void doRender(WaypointEntityRenderer.State state, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light) {
		VertexConsumer buffer = vertexConsumerProvider.getBuffer(LIGHTNING_LAYER);
		Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		Vector3f cameraToPos = new Vector3f(
			(float)(state.x - camera.x),
			(float)(state.y - camera.y),
			(float)(state.z - camera.z)
		);
		Vector3f
			position     = new Vector3f(),
			tangent      = new Vector3f(),
			side         = new Vector3f(),
			prevPosition = new Vector3f(),
			prevTangent  = new Vector3f(),
			prevSide     = new Vector3f();
		float
			size         = 0.0F,
			prevSize     = 0.0F;
		int maxOrbits = BigGlobeMath.roundI(state.health / WaypointEntity.MAX_HEALTH * state.orbits.length);
		for (int orbitIndex = 0; orbitIndex < maxOrbits; orbitIndex++) {
			Orbit orbit = state.orbits[orbitIndex];
			for (int history = 0; history <= 16; history++) {
				if (history != 0) {
					prevPosition.set(position);
					prevTangent.set(tangent);
					prevSide.set(side);
					prevSize = size;
				}
				orbit.getPositionAndVelocity(position, tangent, history);
				position.add(cameraToPos).cross(tangent, side).normalize();
				size = history * 0.0625F;
				size = ((float)(Math.sqrt(size))) * BigGlobeMath.squareF(1.0F - size) * 0.25F;
				if (history != 0) {
					buffer
					.vertex(
						prevPosition.x,
						prevPosition.y + 1.0F,
						prevPosition.z
					)
					.color(
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
					buffer
					.vertex(
						prevPosition.x + prevSide.x * prevSize,
						prevPosition.y + prevSide.x * prevSize + 1.0F,
						prevPosition.z + prevSide.z * prevSize
					)
					.color(
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
					buffer
					.vertex(
						position.x + side.x * size,
						position.y + side.x * size + 1.0F,
						position.z + side.z * size
					)
					.color(
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
					buffer
					.vertex(
						position.x,
						position.y + 1.0F,
						position.z
					)
					.color(
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;



					buffer
					.vertex(
						prevPosition.x - prevSide.x * prevSize,
						prevPosition.y - prevSide.x * prevSize + 1.0F,
						prevPosition.z - prevSide.z * prevSize
					)
					.color(
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
					buffer
					.vertex(
						prevPosition.x,
						prevPosition.y + 1.0F,
						prevPosition.z
					)
					.color(
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
					buffer
					.vertex(
						position.x,
						position.y + 1.0F,
						position.z
					)
					.color(
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
					buffer
					.vertex(
						position.x - side.x * size,
						position.y - side.x * size + 1.0F,
						position.z - side.z * size
					)
					.color(
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F
					)
					#if MC_VERSION < MC_1_21_0 .next() #endif
					;
				}
			}
		}
		#if MC_VERSION < MC_1_21_2
			HyperspaceRendering.markWaypointVisible(state.x, state.y, state.z, state.age, state.health);
		#endif
	}

	#if MC_VERSION < MC_1_20_4
		//1.20.4 has this built in, so this override is only necessary in older versions.
		@Override
		public boolean hasLabel(WaypointEntity entity) {
			return (
				entity.shouldRenderName() || (
					entity.hasCustomName() &&
					MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult hit &&
					hit.getEntity() == entity
				)
			);
		}
	#endif

	#if MC_VERSION < MC_1_21_2

		@Override
		public Identifier getTexture(WaypointEntity entity) {
			return TEXTURE;
		}

	#endif

	@Override
	public WaypointEntityRenderer.State createState() {
		return new State();
	}

	@Override
	public void updateState(WaypointEntity entity, WaypointEntityRenderer.State state, float partialTicks) {
		state.age    = entity.age;
		state.health = entity.health;
		state.orbits = entity.orbits;
	}

	public static class State extends BigGlobeEntityRenderer.State {

		public float age, health;
		public WaypointEntity.Orbit[] orbits;
	}
}